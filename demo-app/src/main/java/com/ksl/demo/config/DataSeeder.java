package com.ksl.demo.config;

import com.genericform.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds sample form schemas and a test submission on startup.
 * Demonstrates how the host app interacts with the library's API.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final FormSchemaManager formSchemaManager;
    private final FormEngine formEngine;

    public DataSeeder(FormSchemaManager formSchemaManager, FormEngine formEngine) {
        this.formSchemaManager = formSchemaManager;
        this.formEngine = formEngine;
    }

    @Override
    public void run(String... args) {
        logger.info("Seeding sample data...");

        // 1. Contact-Us form
        seedContactUsForm();

        // 2. Employee Onboarding form (with nested layout)
        seedEmployeeOnboardingForm();

        // 3. Sample submission against contact-us
        seedSampleSubmission();

        logger.info("Data seeding completed.");
    }

    private void seedContactUsForm() {
        if (formSchemaManager.getSchema("contact-us").isPresent()) {
            logger.info("Schema 'contact-us' already exists, skipping.");
            return;
        }

        FormSchema contactUs = FormSchema.builder()
                .formId("contact-us")
                .formName("Contact Us")
                .version("1.0")
                .components(List.of(
                        FormComponent.builder()
                                .key("fullName").type("textfield").label("Full Name")
                                .input(true)
                                .validate(ComponentValidation.builder().required(true).build())
                                .build(),
                        FormComponent.builder()
                                .key("email").type("email").label("E-mail")
                                .input(true)
                                .validate(ComponentValidation.builder().required(true).build())
                                .build(),
                        FormComponent.builder()
                                .key("subject").type("textfield").label("Subject")
                                .input(true)
                                .validate(ComponentValidation.builder().required(true).build())
                                .build(),
                        FormComponent.builder()
                                .key("message").type("textfield").label("Message")
                                .input(true)
                                .validate(ComponentValidation.builder().required(false).build())
                                .build()))
                .build();

        formSchemaManager.createSchema(contactUs);
        logger.info("Created schema: contact-us");
    }

    private void seedEmployeeOnboardingForm() {
        if (formSchemaManager.getSchema("employee-onboarding").isPresent()) {
            logger.info("Schema 'employee-onboarding' already exists, skipping.");
            return;
        }

        // Build input fields
        FormComponent firstName = FormComponent.builder()
                .key("firstName").type("textfield").label("First Name").input(true)
                .validate(ComponentValidation.builder().required(true).build())
                .build();

        FormComponent lastName = FormComponent.builder()
                .key("lastName").type("textfield").label("Last Name").input(true)
                .validate(ComponentValidation.builder().required(true).build())
                .build();

        FormComponent email = FormComponent.builder()
                .key("email").type("email").label("Email").input(true)
                .validate(ComponentValidation.builder().required(true).build())
                .build();

        FormComponent street = FormComponent.builder()
                .key("street").type("textfield").label("Street").input(true)
                .validate(ComponentValidation.builder().required(true).build())
                .build();

        FormComponent city = FormComponent.builder()
                .key("city").type("textfield").label("City").input(true)
                .validate(ComponentValidation.builder().required(true).build())
                .build();

        FormComponent postalCode = FormComponent.builder()
                .key("postalCode").type("textfield").label("Postal Code").input(true)
                .validate(ComponentValidation.builder()
                        .required(true).pattern("^[A-Za-z0-9\\- ]{3,10}$").build())
                .build();

        // Wrap in a fieldset layout (like Form.io would)
        FormComponent addressFieldset = FormComponent.builder()
                .key("addressSection").type("fieldset").label("Address").input(false)
                .components(List.of(street, city, postalCode))
                .build();

        // Top-level well wrapping everything
        FormComponent well = FormComponent.builder()
                .key("mainWell").type("well").input(false)
                .components(List.of(firstName, lastName, email, addressFieldset))
                .build();

        FormSchema onboarding = FormSchema.builder()
                .formId("employee-onboarding")
                .formName("Employee Onboarding")
                .version("1.0")
                .components(List.of(well))
                .build();

        formSchemaManager.createSchema(onboarding);
        logger.info("Created schema: employee-onboarding");
    }

    private void seedSampleSubmission() {
        SubmissionResult result = formEngine.process("contact-us", Map.of(
                "fullName", "John Doe",
                "email", "john.doe@example.com",
                "subject", "Hello",
                "message", "This is a sample submission."));
        logger.info("Sample submission result: valid={}", result.isValid());
    }
}
