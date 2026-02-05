#!/usr/bin/env python3
"""
Verify imported data in HydroSpark database
"""

import mysql.connector
from datetime import datetime

DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'Hydrospark',
    'password': 'Hydrospark123',
    'database': 'hydrospark'
}

def main():
    print("🔍 Verifying HydroSpark Database...\n")
    
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # Count customers
    cursor.execute("SELECT COUNT(*) FROM customers")
    customer_count = cursor.fetchone()[0]
    print(f"✓ Total customers: {customer_count}")
    
    # Count meters
    cursor.execute("SELECT COUNT(*) FROM meters")
    meter_count = cursor.fetchone()[0]
    print(f"✓ Total meters: {meter_count}")
    
    # Count meter readings
    cursor.execute("SELECT COUNT(*) FROM meter_readings")
    reading_count = cursor.fetchone()[0]
    print(f"✓ Total meter readings: {reading_count:,}")
    
    # Date range of readings
    cursor.execute("""
        SELECT MIN(reading_date), MAX(reading_date) 
        FROM meter_readings
    """)
    min_date, max_date = cursor.fetchone()
    print(f"✓ Reading date range: {min_date} to {max_date}")
    
    # Sample customers
    print("\n📋 Sample Customers:")
    cursor.execute("""
        SELECT account_number, first_name, last_name, email, customer_type 
        FROM customers 
        LIMIT 5
    """)
    for row in cursor.fetchall():
        print(f"   {row[0]}: {row[1]} {row[2]} ({row[4]})")
    
    # Sample meters
    print("\n📊 Sample Meters:")
    cursor.execute("""
        SELECT m.external_location_id, c.first_name, c.last_name, m.status
        FROM meters m
        JOIN customers c ON m.customer_id = c.id
        LIMIT 5
    """)
    for row in cursor.fetchall():
        print(f"   Location {row[0]}: {row[1]} {row[2]} ({row[3]})")
    
    # Recent readings
    print("\n📈 Recent Meter Readings:")
    cursor.execute("""
        SELECT mr.reading_date, mr.usage_ccf, c.first_name, c.last_name
        FROM meter_readings mr
        JOIN meters m ON mr.meter_id = m.id
        JOIN customers c ON m.customer_id = c.id
        ORDER BY mr.reading_date DESC
        LIMIT 5
    """)
    for row in cursor.fetchall():
        print(f"   {row[0]}: {row[2]} {row[3]} used {row[1]} CCF")
    
    # Usage statistics
    print("\n📊 Usage Statistics:")
    cursor.execute("""
        SELECT 
            AVG(usage_ccf) as avg_usage,
            MIN(usage_ccf) as min_usage,
            MAX(usage_ccf) as max_usage,
            SUM(usage_ccf) as total_usage
        FROM meter_readings
    """)
    stats = cursor.fetchone()
    print(f"   Average daily usage: {stats[0]:.2f} CCF")
    print(f"   Min usage: {stats[1]:.2f} CCF")
    print(f"   Max usage: {stats[2]:.2f} CCF")
    print(f"   Total usage (all time): {stats[3]:,.2f} CCF")
    
    cursor.close()
    conn.close()
    
    print("\n✅ Verification complete!")

if __name__ == '__main__':
    main()
