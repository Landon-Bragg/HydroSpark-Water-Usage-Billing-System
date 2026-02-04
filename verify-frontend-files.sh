#!/bin/bash
# Frontend File Verification Script
# Run this from your project root to check if all required files are in place

echo "=========================================="
echo "HydroSpark Frontend File Verification"
echo "=========================================="
echo ""

MISSING_FILES=0
FRONTEND_DIR="frontend/src"

check_file() {
    local file=$1
    local required=$2
    
    if [ -f "$FRONTEND_DIR/$file" ]; then
        echo "✅ $file"
    else
        echo "❌ MISSING: $file"
        if [ "$required" = "true" ]; then
            MISSING_FILES=$((MISSING_FILES + 1))
        fi
    fi
}

check_dir() {
    local dir=$1
    
    if [ -d "$FRONTEND_DIR/$dir" ]; then
        echo "✅ $dir/ (directory exists)"
    else
        echo "❌ MISSING: $dir/ (directory)"
        MISSING_FILES=$((MISSING_FILES + 1))
    fi
}

echo "Core Files:"
check_file "index.tsx" "true"
check_file "App.tsx" "true"
check_file "App.js" "false"  # Should NOT exist

if [ -f "$FRONTEND_DIR/App.js" ]; then
    echo "⚠️  WARNING: App.js exists and should be DELETED!"
    MISSING_FILES=$((MISSING_FILES + 1))
fi

echo ""
echo "Contexts:"
check_dir "contexts"
check_file "contexts/AuthContext.tsx" "true"

echo ""
echo "Services:"
check_dir "services"
check_file "services/api.ts" "true"
check_file "services/authService.ts" "true"
check_file "services/customerService.ts" "true"
check_file "services/staffService.ts" "true"
check_file "services/billingPeriodService.ts" "true"

echo ""
echo "Components - Auth:"
check_dir "components/Auth"
check_file "components/Auth/Login.tsx" "true"

echo ""
echo "Components - Customer:"
check_dir "components/Customer"
check_file "components/Customer/Dashboard.tsx" "true"
check_file "components/Customer/BillHistory.tsx" "true"

echo ""
echo "Components - Staff:"
check_dir "components/Staff"
check_file "components/Staff/Dashboard.tsx" "true"
check_file "components/Staff/BillingTab.tsx" "true"
check_file "components/Staff/CustomersTab.tsx" "true"
check_file "components/Staff/RatesTab.tsx" "true"
check_file "components/Staff/ImportTab.tsx" "true"
check_file "components/Staff/AnomaliesTab.tsx" "true"

echo ""
echo "=========================================="
if [ $MISSING_FILES -eq 0 ]; then
    echo "✅ ALL FILES PRESENT!"
    echo "Your frontend should be ready to run."
else
    echo "❌ $MISSING_FILES ISSUES FOUND"
    echo "Please review the missing files above."
fi
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Delete App.js if it exists: rm frontend/src/App.js"
echo "2. Make sure index.tsx imports App (not App.js)"
echo "3. Run: npm start"
echo "4. Visit: http://localhost:3000"
echo "5. You should see the LOGIN page (not 'Frontend is running')"