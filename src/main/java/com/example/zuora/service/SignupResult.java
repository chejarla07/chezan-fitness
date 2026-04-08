package com.example.zuora.service;

import com.example.zuora.model.User;

/**
 * Result object for user signup containing user data and Zuora creation status
 */
public class SignupResult {
    private final User user;
    private final boolean zuoraSuccess;
    private final String zuoraAccountId;
    private final String zuoraAccountNumber;
    private final String billToContactId;
    private final String soldToContactId;
    private final String errorMessage;

    private SignupResult(Builder builder) {
        this.user = builder.user;
        this.zuoraSuccess = builder.zuoraSuccess;
        this.zuoraAccountId = builder.zuoraAccountId;
        this.zuoraAccountNumber = builder.zuoraAccountNumber;
        this.billToContactId = builder.billToContactId;
        this.soldToContactId = builder.soldToContactId;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public User getUser() {
        return user;
    }

    public boolean isZuoraSuccess() {
        return zuoraSuccess;
    }

    public String getZuoraAccountId() {
        return zuoraAccountId;
    }

    public String getZuoraAccountNumber() {
        return zuoraAccountNumber;
    }

    public String getBillToContactId() {
        return billToContactId;
    }

    public String getSoldToContactId() {
        return soldToContactId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private User user;
        private boolean zuoraSuccess;
        private String zuoraAccountId;
        private String zuoraAccountNumber;
        private String billToContactId;
        private String soldToContactId;
        private String errorMessage;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder zuoraSuccess(boolean zuoraSuccess) {
            this.zuoraSuccess = zuoraSuccess;
            return this;
        }

        public Builder zuoraAccountId(String zuoraAccountId) {
            this.zuoraAccountId = zuoraAccountId;
            return this;
        }

        public Builder zuoraAccountNumber(String zuoraAccountNumber) {
            this.zuoraAccountNumber = zuoraAccountNumber;
            return this;
        }

        public Builder billToContactId(String billToContactId) {
            this.billToContactId = billToContactId;
            return this;
        }

        public Builder soldToContactId(String soldToContactId) {
            this.soldToContactId = soldToContactId;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public SignupResult build() {
            return new SignupResult(this);
        }
    }
}
