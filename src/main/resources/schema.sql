-- Chezan Fitness Database Schema
-- Fitness Membership Platform with Zuora Integration

-- Users table (for authentication)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    phone TEXT,
    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'CUSTOMER')),
    zuora_account_id TEXT,
    zuora_account_number TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table (fitness offerings)
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_product_id TEXT UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL CHECK(category IN ('MEMBERSHIP', 'CLASS', 'ADD_ON', 'PERSONAL_TRAINING')),
    status TEXT DEFAULT 'Active' CHECK(status IN ('Active', 'Inactive')),
    effective_start_date DATE,
    effective_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rate Plans table (pricing tiers)
CREATE TABLE IF NOT EXISTS rate_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_rate_plan_id TEXT UNIQUE,
    product_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'Active' CHECK(status IN ('Active', 'Inactive')),
    effective_start_date DATE,
    effective_end_date DATE,
    billing_period TEXT CHECK(billing_period IN ('Month', 'Quarter', 'Annual')),
    billing_period_value INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Rate Plan Charges (pricing details)
CREATE TABLE IF NOT EXISTS rate_plan_charges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_charge_id TEXT UNIQUE,
    rate_plan_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    charge_type TEXT NOT NULL CHECK(charge_type IN ('Recurring', 'OneTime', 'Usage')),
    charge_model TEXT CHECK(charge_model IN ('FlatFee', 'PerUnit', 'Tiered')),
    amount DECIMAL(10, 2),
    currency TEXT DEFAULT 'USD',
    billing_timing TEXT CHECK(billing_timing IN ('InAdvance', 'InArrears')),
    billing_day TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id)
);

-- Subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_subscription_id TEXT UNIQUE,
    zuora_subscription_number TEXT,
    user_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    status TEXT DEFAULT 'Active' CHECK(status IN ('Active', 'Cancelled', 'Suspended', 'Expired')),
    rate_plan_id INTEGER NOT NULL,
    contract_effective_date DATE NOT NULL,
    service_activation_date DATE,
    term_start_date DATE NOT NULL,
    term_end_date DATE,
    initial_term INTEGER,
    initial_term_period_type TEXT,
    renewal_term INTEGER,
    auto_renew BOOLEAN DEFAULT TRUE,
    cancel_reason TEXT,
    cancellation_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id)
);

-- Payment Methods table
CREATE TABLE IF NOT EXISTS payment_methods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_payment_method_id TEXT UNIQUE,
    user_id INTEGER NOT NULL,
    account_id INTEGER,
    type TEXT NOT NULL CHECK(type IN ('CreditCard', 'ACH', 'PayPal', 'ApplePay', 'GooglePay')),
    is_default BOOLEAN DEFAULT FALSE,
    card_last_four TEXT,
    card_brand TEXT,
    card_expiration_month INTEGER,
    card_expiration_year INTEGER,
    ach_bank_name TEXT,
    ach_account_last_four TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_invoice_id TEXT UNIQUE,
    zuora_invoice_number TEXT,
    user_id INTEGER NOT NULL,
    account_id INTEGER,
    subscription_id INTEGER,
    amount DECIMAL(10, 2) NOT NULL,
    balance DECIMAL(10, 2),
    tax_amount DECIMAL(10, 2),
    total_amount DECIMAL(10, 2) NOT NULL,
    status TEXT CHECK(status IN ('Draft', 'Posted', 'Paid', 'Voided', 'WriteOff')),
    invoice_date DATE NOT NULL,
    due_date DATE,
    paid_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_payment_id TEXT UNIQUE,
    zuora_payment_number TEXT,
    user_id INTEGER NOT NULL,
    invoice_id INTEGER,
    payment_method_id INTEGER,
    amount DECIMAL(10, 2) NOT NULL,
    currency TEXT DEFAULT 'USD',
    status TEXT CHECK(status IN ('Processing', 'Processed', 'Error', 'Voided')),
    payment_date DATE NOT NULL,
    reference_number TEXT,
    gateway_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
);

-- User Activity Log
CREATE TABLE IF NOT EXISTS user_activity_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id TEXT,
    details TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- API Response Log (existing table extended)
CREATE TABLE IF NOT EXISTS zuora_api_responses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    api_endpoint TEXT,
    process_id TEXT,
    request_id TEXT,
    account_id TEXT,
    account_number TEXT,
    subscription_id TEXT,
    success TEXT,
    error_code TEXT,
    error_message TEXT,
    raw_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Discounts table
CREATE TABLE IF NOT EXISTS discounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zuora_discount_id TEXT UNIQUE,
    discount_code TEXT UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    discount_type TEXT NOT NULL CHECK(discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_percentage DECIMAL(5, 2),
    discount_amount DECIMAL(10, 2),
    discount_level TEXT CHECK(discount_level IN ('RATEPLAN', 'SUBSCRIPTION', 'ACCOUNT')),
    apply_to TEXT CHECK(apply_to IN ('ONETIME', 'RECURRING', 'USAGE', 'ALL')),
    product_id INTEGER,
    rate_plan_id INTEGER,
    start_date DATE,
    end_date DATE,
    max_redemptions INTEGER,
    current_redemptions INTEGER DEFAULT 0,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id)
);

-- Waiver Acceptances table
CREATE TABLE IF NOT EXISTS waiver_acceptances (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    waiver_type TEXT NOT NULL,
    waiver_version TEXT NOT NULL,
    accepted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address TEXT,
    user_agent TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Health Questionnaires table
CREATE TABLE IF NOT EXISTS health_questionnaires (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    has_heart_condition BOOLEAN,
    has_chest_pain BOOLEAN,
    has_dizziness BOOLEAN,
    has_blood_pressure_issues BOOLEAN,
    has_joint_problems BOOLEAN,
    has_other_conditions BOOLEAN,
    other_conditions_details TEXT,
    takes_medications BOOLEAN,
    medication_details TEXT,
    has_allergies BOOLEAN,
    allergy_details TEXT,
    has_surgeries BOOLEAN,
    surgery_details TEXT,
    additional_notes TEXT,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Check-ins table
CREATE TABLE IF NOT EXISTS check_ins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    check_out_time TIMESTAMP,
    location TEXT,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Suspended Subscription table (for pause/resume tracking)
CREATE TABLE IF NOT EXISTS subscription_suspensions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subscription_id INTEGER NOT NULL,
    suspend_date DATE NOT NULL,
    resume_date DATE,
    suspend_periods INTEGER,
    status TEXT DEFAULT 'SUSPENDED' CHECK(status IN ('SUSPENDED', 'RESUMED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_zuora_account ON users(zuora_account_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_rate_plans_product ON rate_plans(product_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_payment_methods_user ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_invoices_user ON invoices(user_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_discounts_code ON discounts(discount_code);
CREATE INDEX IF NOT EXISTS idx_discounts_status ON discounts(status);
CREATE INDEX IF NOT EXISTS idx_waivers_user ON waiver_acceptances(user_id);
CREATE INDEX IF NOT EXISTS idx_questionnaires_user ON health_questionnaires(user_id);
CREATE INDEX IF NOT EXISTS idx_checkins_user ON check_ins(user_id);
