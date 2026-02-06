#!/usr/bin/env python3
"""
HydroSpark Water Usage Data Import Script
==========================================
Imports daily water usage data from CSV into the MySQL database.

Expected CSV Columns:
- Customer Name
- Mailing Address
- Location ID
- Customer Type
- Cycle Number
- Customer Phone Number
- Business Name
- Facility Name
- Year
- Month
- Day
- Daily Water Usage (CCF)

Usage:
    python import_water_usage.py <csv_file_path>
"""

import pandas as pd
import mysql.connector
from mysql.connector import Error
import uuid
from datetime import datetime, date
import sys
import os
from typing import Dict, Tuple
import re

# Database Configuration
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3310,
    'database': 'hydrospark_db',
    'user': 'root',
    'password': 'rootpassword123',
    'charset': 'utf8mb4'
}

STATE_MAP = {
    "TEXAS": "TX",
    # add more if you expect other states
}

def normalize_state(raw: str) -> str:
    if raw is None or pd.isna(raw):
        return "TX"
    s = re.sub(r"[^A-Za-z]", "", str(raw)).upper()
    if len(s) == 2:
        return s
    return STATE_MAP.get(s, "TX")
class WaterUsageImporter:
    def __init__(self, csv_file_path: str):
        self.csv_file_path = csv_file_path
        self.connection = None
        self.cursor = None
        self.import_run_id = str(uuid.uuid4())

        self.max_fail_rate = 0.01  # stop if >1% failing
        self.min_rows_before_abort = 5000

        
        # Caches to avoid repeated database lookups
        self.customer_cache = {}
        self.meter_cache = {}
        
        # Statistics
        self.stats = {
            'rows_processed': 0,
            'rows_imported': 0,
            'rows_failed': 0,
            'customers_created': 0,
            'meters_created': 0,
            'readings_inserted': 0
        }
    
    def connect_db(self):
        """Establish database connection."""
        try:
            self.connection = mysql.connector.connect(**DB_CONFIG)
            self.cursor = self.connection.cursor(dictionary=True)
            print(f"✓ Connected to database: {DB_CONFIG['database']}")
            return True
        except Error as e:
            print(f"✗ Error connecting to database: {e}")
            return False
    
    def close_db(self):
        """Close database connection."""
        if self.cursor:
            self.cursor.close()
        if self.connection:
            self.connection.close()
        print("✓ Database connection closed")
    
    def create_import_run(self):
        """Create an import run record."""
        try:
            query = """
                INSERT INTO import_runs (id, source_type, source_file_name, status, imported_by)
                VALUES (%s, 'CSV', %s, 'IN_PROGRESS', 'SYSTEM')
            """
            self.cursor.execute(query, (self.import_run_id, os.path.basename(self.csv_file_path)))
            self.connection.commit()
            print(f"✓ Created import run: {self.import_run_id}")
        except Error as e:
            print(f"✗ Error creating import run: {e}")
    
    def update_import_run(self, status: str, error_message: str = None):
        """Update import run with final statistics."""
        try:
            query = """
                UPDATE import_runs 
                SET status = %s, 
                    rows_processed = %s,
                    rows_imported = %s,
                    rows_failed = %s,
                    error_message = %s,
                    completed_at = NOW()
                WHERE id = %s
            """
            self.cursor.execute(query, (
                status,
                self.stats['rows_processed'],
                self.stats['rows_imported'],
                self.stats['rows_failed'],
                error_message,
                self.import_run_id
            ))
            self.connection.commit()
            print(f"✓ Updated import run status: {status}")
        except Error as e:
            print(f"✗ Error updating import run: {e}")
    
    def parse_address(self, address: str) -> Tuple[str, str, str, str]:
        """Parse address into components."""
        if not address or pd.isna(address):
            return '', '', 'TX', ''

        parts = str(address).split(',')

        if len(parts) >= 3:
            street = parts[0].strip()
            city = parts[1].strip()
            state_zip = parts[2].strip().split()
            state = normalize_state(state_zip[0] if len(state_zip) > 0 else "TX")
            zip_code = state_zip[1] if len(state_zip) > 1 else ''
            return street, city, state, zip_code
        elif len(parts) == 2:
            street = parts[0].strip()
            city = parts[1].strip()
            return street, city, 'TX', ''
        else:
            return str(address).strip(), '', 'TX', ''

    
    def get_or_create_customer(self, row: pd.Series) -> str:
        """Get existing customer or create new one."""
        # Create a unique key for the customer
        location_id = str(row.get('Location ID', ''))
        
        if location_id in self.customer_cache:
            return self.customer_cache[location_id]
        
        # Try to find existing customer by location ID
        query = """
            SELECT c.id FROM customers c
            JOIN meters m ON m.customer_id = c.id
            WHERE m.external_location_id = %s
            LIMIT 1
        """
        self.cursor.execute(query, (location_id,))
        result = self.cursor.fetchone()
        
        if result:
            customer_id = result['id']
            self.customer_cache[location_id] = customer_id
            return customer_id
        
        # Create new customer
        customer_id = str(uuid.uuid4())
        
        # Parse customer name
        customer_name = str(row.get('Customer Name', 'Unknown'))
        name_parts = customer_name.split()
        first_name = name_parts[0] if len(name_parts) > 0 else 'Unknown'
        last_name = ' '.join(name_parts[1:]) if len(name_parts) > 1 else 'Customer'
        
        # Parse address
        mailing_address = str(row.get('Mailing Address', ''))
        street, city, state, zip_code = self.parse_address(mailing_address)
        
        # Account number from Location ID
        account_number = f"ACC-{location_id}"
        
        # Customer type
        customer_type = str(row.get('Customer Type', 'RESIDENTIAL')).upper()
        if customer_type not in ['RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL']:
            customer_type = 'RESIDENTIAL'
        
        # Business name
        business_name = row.get('Business Name', None)
        if pd.isna(business_name):
            business_name = None
        
        # Email (generate placeholder)
        email = f"customer.{location_id}@hydrospark.com"
        
        # Phone
        phone = row.get('Customer Phone Number', None)
        if pd.isna(phone):
            phone = None
        
        # Cycle number
        cycle_number = row.get('Cycle Number', 1)
        if pd.isna(cycle_number):
            cycle_number = 1
        
        try:
            insert_query = """
                INSERT INTO customers (
                    id, account_number, first_name, last_name, business_name,
                    email, phone, service_address, city, state, zip_code,
                    mailing_address_line1, mailing_city, mailing_state, mailing_zip,
                    customer_type, billing_cycle_number, is_active
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, TRUE
                )
            """
            self.cursor.execute(insert_query, (
                customer_id, account_number, first_name, last_name, business_name,
                email, phone, street, city, state, zip_code,
                street, city, state, zip_code,
                customer_type, int(cycle_number)
            ))
            
            self.stats['customers_created'] += 1
            self.customer_cache[location_id] = customer_id
            
            return customer_id
            
        except Error as e:
            print(f"✗ Error creating customer for Location {location_id}: {e}")
            return None
    
    def get_or_create_meter(self, row: pd.Series, customer_id: str) -> str:
        """Get existing meter or create new one."""
        location_id = str(row.get('Location ID', ''))
        
        if location_id in self.meter_cache:
            return self.meter_cache[location_id]
        
        # Try to find existing meter
        query = "SELECT id FROM meters WHERE external_location_id = %s LIMIT 1"
        self.cursor.execute(query, (location_id,))
        result = self.cursor.fetchone()
        
        if result:
            meter_id = result['id']
            self.meter_cache[location_id] = meter_id
            return meter_id
        
        # Create new meter
        meter_id = str(uuid.uuid4())
        
        # Parse address
        mailing_address = str(row.get('Mailing Address', ''))
        street, city, state, zip_code = self.parse_address(mailing_address)
        
        # Facility name
        facility_name = row.get('Facility Name', None)
        if pd.isna(facility_name):
            facility_name = None
        
        try:
            insert_query = """
                INSERT INTO meters (
                    id, customer_id, external_location_id, meter_number,
                    service_address_line1, service_city, service_state, service_zip,
                    facility_name, meter_type, status
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, 'STANDARD', 'ACTIVE'
                )
            """
            meter_number = f"M-{location_id}"
            
            self.cursor.execute(insert_query, (
                meter_id, customer_id, location_id, meter_number,
                street, city, state, zip_code, facility_name
            ))
            
            self.stats['meters_created'] += 1
            self.meter_cache[location_id] = meter_id
            
            return meter_id
            
        except Error as e:
            print(f"✗ Error creating meter for Location {location_id}: {e}")
            return None
    
    def insert_daily_reading(self, row: pd.Series, customer_id: str, meter_id: str):
        """Insert a daily water reading."""
        try:
            year = int(row['Year'])
            month = int(row['Month'])
            day = int(row['Day'])
            reading_date = date(year, month, day)
            
            usage_ccf = float(row['Daily Water Usage (CCF)'])
            location_id = str(row['Location ID'])
            
            insert_query = """
                INSERT INTO daily_water_readings (
                    id, customer_id, meter_id, external_location_id,
                    reading_year, reading_month, reading_day, reading_date,
                    daily_usage_ccf, import_run_id
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                )
                ON DUPLICATE KEY UPDATE
                    daily_usage_ccf = VALUES(daily_usage_ccf),
                    import_run_id = VALUES(import_run_id)
            """
            
            reading_id = uuid.uuid4().int % (10**10)  # Generate numeric ID
            
            self.cursor.execute(insert_query, (
                reading_id, customer_id, meter_id, location_id,
                year, month, day, reading_date, usage_ccf, self.import_run_id
            ))
            
            self.stats['readings_inserted'] += 1
            
        except Exception as e:
            print(f"✗ Error inserting reading: {e}")
            self.stats['rows_failed'] += 1
    
    def process_csv(self):
        """Process the CSV file and import data."""
        print(f"\n{'='*60}")
        print(f"Starting import from: {self.csv_file_path}")
        print(f"{'='*60}\n")
        
        try:
            # Read CSV
            print("Reading CSV file...")
            df = pd.read_csv(self.csv_file_path)
            total_rows = len(df)
            print(f"✓ Found {total_rows:,} rows in CSV\n")
            
            # Process in batches
            batch_size = 5000
            batch_count = 0
            
            for index, row in df.iterrows():
                self.stats['rows_processed'] += 1
                
                try:
                    # Get or create customer
                    customer_id = self.get_or_create_customer(row)
                    if not customer_id:
                        self.stats['rows_failed'] += 1
                        continue
                    
                    # Get or create meter
                    meter_id = self.get_or_create_meter(row, customer_id)
                    if not meter_id:
                        self.stats['rows_failed'] += 1
                        continue
                    
                    # Insert daily reading
                    self.insert_daily_reading(row, customer_id, meter_id)
                    self.stats['rows_imported'] += 1
                    
                    # Commit in batches
                    if self.stats['rows_processed'] % batch_size == 0:
                        self.connection.commit()
                        batch_count += 1
                        progress = (self.stats['rows_processed'] / total_rows) * 100
                        print(f"Progress: {self.stats['rows_processed']:,}/{total_rows:,} rows ({progress:.1f}%) - "
                              f"Customers: {self.stats['customers_created']}, "
                              f"Meters: {self.stats['meters_created']}, "
                              f"Readings: {self.stats['readings_inserted']}")
                
                except Exception as e:
                    print(f"✗ Error processing row {index}: {e}")
                    self.stats['rows_failed'] += 1
            
            # Final commit
            self.connection.commit()
            
            print(f"\n{'='*60}")
            print("Import Summary:")
            print(f"{'='*60}")
            print(f"Total Rows Processed:    {self.stats['rows_processed']:,}")
            print(f"Rows Imported:           {self.stats['rows_imported']:,}")
            print(f"Rows Failed:             {self.stats['rows_failed']:,}")
            print(f"Customers Created:       {self.stats['customers_created']:,}")
            print(f"Meters Created:          {self.stats['meters_created']:,}")
            print(f"Readings Inserted:       {self.stats['readings_inserted']:,}")
            print(f"{'='*60}\n")

            if self.stats['rows_processed'] >= self.min_rows_before_abort:
                fail_rate = self.stats['rows_failed'] / self.stats['rows_processed']
                if fail_rate > self.max_fail_rate:
                    raise RuntimeError(f"Aborting: fail rate too high ({fail_rate:.2%})")

            
            return True
            
        except Exception as e:
            print(f"\n✗ Fatal error during import: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def run(self):
        """Main execution method."""
        if not os.path.exists(self.csv_file_path):
            print(f"✗ CSV file not found: {self.csv_file_path}")
            return False
        
        if not self.connect_db():
            return False
        
        try:
            self.create_import_run()
            success = self.process_csv()
            
            if success:
                self.update_import_run('COMPLETED')
                print("✓ Import completed successfully!")
            else:
                self.update_import_run('FAILED', 'See logs for details')
                print("✗ Import failed with errors")
            
            return success
            
        except Exception as e:
            print(f"\n✗ Unexpected error: {e}")
            import traceback
            traceback.print_exc()
            self.update_import_run('FAILED', str(e))
            return False
            
        finally:
            self.close_db()


def main():
    if len(sys.argv) < 2:
        print("Usage: python import_water_usage.py <csv_file_path>")
        print("\nExample:")
        print("  python import_water_usage.py water_usage_data.csv")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    importer = WaterUsageImporter(csv_file)
    success = importer.run()
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
