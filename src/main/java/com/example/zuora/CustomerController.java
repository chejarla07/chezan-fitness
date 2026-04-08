package com.example.zuora;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class CustomerController {

    @Autowired
    private ZuoraService zuoraService;

    @GetMapping("/customer/create")
    public String showForm(Model model) {
        CustomerForm form = new CustomerForm();
        // Set default values
        form.setName("Acme Technology Solutions Inc.");
        form.setCurrency("USD");
        form.setBillToFirstName("John");
        form.setBillToLastName("Doe");
        form.setBillToEmail("john.doe@acmetech.com");
        form.setBillToPhone("+1-555-123-4567");
        form.setBillToAddress1("123 Innovation Drive");
        form.setBillToAddress2("Suite 456");
        form.setBillToCity("San Francisco");
        form.setBillToState("California");
        form.setBillToCountry("United States");
        form.setBillToZipCode("94105");
        form.setSoldToFirstName("Jane");
        form.setSoldToLastName("Smith");
        form.setSoldToEmail("jane.smith@acmetech.com");
        form.setSoldToPhone("+1-555-987-6543");
        form.setSoldToAddress1("123 Innovation Drive");
        form.setSoldToAddress2("Suite 456");
        form.setSoldToCity("San Francisco");
        form.setSoldToState("California");
        form.setSoldToCountry("United States");
        form.setSoldToZipCode("94105");
        form.setPaymentTerm("Net 30");
        form.setBatch("Batch1");
        form.setBillCycleDay(1);
        form.setAutoPay(false);
        form.setNotes("Enterprise customer - VIP support level");
        form.setConsumerType("B2B");

        model.addAttribute("customerForm", form);
        return "customer-form";
    }

    @PostMapping("/create")
    public String createCustomer(@ModelAttribute CustomerForm form, Model model) {
        try {
            String apiResponse = zuoraService.createCustomerAccount(form);
            model.addAttribute("submittedData", form);
            model.addAttribute("apiResponse", apiResponse);
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("submittedData", form);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
        }
        return "result";
    }
}