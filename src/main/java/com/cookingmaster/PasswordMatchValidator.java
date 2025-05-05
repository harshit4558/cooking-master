package com.cookingmaster;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

@FacesValidator("passwordMatchValidator")
public class PasswordMatchValidator implements Validator<String> {

    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        String password = (String) component.getAttributes().get("password");
        
        if (value != null && !value.equals(password)) {
            throw new ValidatorException(new FacesMessage("Passwords do not match"));
        }
    }
} 