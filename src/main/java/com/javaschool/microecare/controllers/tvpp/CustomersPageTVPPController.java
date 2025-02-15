package com.javaschool.microecare.controllers.tvpp;


import com.javaschool.microecare.commonentitymanagement.dao.EntityCannotBeSavedException;
import com.javaschool.microecare.commonentitymanagement.service.CommonEntityService;
import com.javaschool.microecare.contractmanagement.service.ContractsService;
import com.javaschool.microecare.contractmanagement.viewmodel.MobileNumberView;
import com.javaschool.microecare.customermanagement.dao.Customer;
import com.javaschool.microecare.customermanagement.dto.*;
import com.javaschool.microecare.customermanagement.service.CustomersService;
import com.javaschool.microecare.customermanagement.service.PassportType;
import com.javaschool.microecare.customermanagement.viewmodel.CustomerView;
import com.javaschool.microecare.utils.EntityActions;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for Customers page in TVPP.
 */
@Controller
@RequestMapping("${endpoints.tvpp.customers.controller_path}")
@PropertySource("messages.properties")
@SessionAttributes("processProgress")
public class CustomersPageTVPPController {
    @Resource(name = "sessionScopedCustomerDTO")
    CustomerDTO sessionScopedCustomerDTO;

    @Resource(name = "sessionScopedCustomerView")
    CustomerView sessionScopedCustomerView;

    @Value("${directory.templates.tvpp.customers}")
    private String templateFolder;
    @Value("${endpoints.tvpp.customers.controller_path}")
    private String controllerPath;
    @Value("${endpoints.tvpp.customers.path.personal_data}")
    private String personalDataPath;
    @Value("${endpoints.tvpp.customers.path.address}")
    private String addressPath;
    @Value("${endpoints.tvpp.customers.path.passport}")
    private String passportPath;
    @Value("${endpoints.tvpp.customers.path.login}")
    private String loginPath;
    @Value("${endpoints.tvpp.customers.path.overview}")
    private String overviewPath;
    @Value("${endpoints.tvpp.customers.path.personal_data.edit}")
    private String personalDataEditPath;
    @Value("${endpoints.tvpp.customers.path.address.edit}")
    private String addressEditPath;
    @Value("${endpoints.tvpp.customers.path.passport.edit}")
    private String passportEditPath;
    @Value("${endpoints.tvpp.customers.path.login.edit}")
    private String loginEditPath;
    @Value("${endpoints.tvpp.customers.path.overview.edit}")
    private String overviewEditPath;
    @Value("${endpoints.tvpp.basket.controller_path}")
    private String basketControllerPath;
    @Value("${general.field.not_date.msg}")
    private String notDateErrorMessage;
    @Value("${customer.delete.in_contract.msg}")
    private String customerDeleteInContractMessage;
    @Value("${general.field.not_int.msg}")
    private String fieldNotIntMessage;

    private final String ENTITY_NAME = "Customer";

    private boolean successfulAction = false;
    private long successId;
    private boolean viewDetails = false;
    private CustomerView displayedCustomer;
    private String errorMessage;
    private EntityActions action;
    private List<MobileNumberView> customersMobileNumbers;


    final CommonEntityService commonEntityService;
    final CustomersService customersService;
    final ContractsService contractsService;

    public CustomersPageTVPPController(CommonEntityService commonEntityService, CustomersService customersService, ContractsService contractsService) {
        this.commonEntityService = commonEntityService;
        this.customersService = customersService;
        this.contractsService = contractsService;
    }


    @ModelAttribute
    public void setCommonAttributes(Model model) {
        commonEntityService.setPathsAttributes(model, controllerPath);
        //paths for new customer process
        model.addAttribute("personalDataPath", controllerPath + personalDataPath);
        model.addAttribute("addressPath", controllerPath + addressPath);
        model.addAttribute("passportPath", controllerPath + passportPath);
        model.addAttribute("loginPath", controllerPath + loginPath);
        model.addAttribute("submitPath", controllerPath + overviewPath);
        model.addAttribute("overviewPath", overviewPath);

        //paths for edit customer process
        model.addAttribute("personalDataEditPath", controllerPath + personalDataEditPath);
        model.addAttribute("addressEditPath", controllerPath + addressEditPath);
        model.addAttribute("passportEditPath", controllerPath + passportEditPath);
        model.addAttribute("loginEditPath", controllerPath + loginEditPath);
        model.addAttribute("overviewEditPath", controllerPath + overviewEditPath);

        //session customer dto to use in new and update customer processes
        model.addAttribute("customerDTO", sessionScopedCustomerDTO);

    }

    private void setAllCustomersModel(Model model) {
        model.addAttribute("customers", customersService.getAllCustomerViews());
        if (successfulAction) {
            commonEntityService.setSuccessfulActionModel(model, ENTITY_NAME, action, successId);
        }
        if (viewDetails) {
            model.addAttribute("viewCustomerDetails", true);
            model.addAttribute("displayedCustomer", displayedCustomer);
            model.addAttribute("customersMobileNumbers", customersMobileNumbers);
            viewDetails = false;
        }
        if (errorMessage != null) {
            commonEntityService.setErrorModel(model, ENTITY_NAME, errorMessage, action);
        }
    }

    @GetMapping
    public String getCustomersPage(Model model, @RequestParam(required = false) Boolean cancel) {
        setAllCustomersModel(model);
        successfulAction = false;
        errorMessage = null;
        action = null;
        viewDetails = false;
        customersMobileNumbers = new ArrayList<>();
        //подумать, нужен ли мне кэнсел, или можно всегда ресетать сессию
        /*
        if (cancel != null && cancel) {
            customersService.resetCustomerDTO(sessionScopedCustomerDTO);
        }*/
        customersService.resetCustomerDTO(sessionScopedCustomerDTO);
        customersService.resetCustomerView(sessionScopedCustomerView);
        model.addAttribute("processProgress", false);
        return templateFolder + "customers";
    }

    @GetMapping("${endpoints.tvpp.entity.path.new}")
    public String getCreateNewCustomer(PersonalDataDTO personalDataDTO, Model model) {
        model.addAttribute("processProgress", true);
        return "redirect:" + controllerPath + personalDataPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.personal_data}")
    public String showNewCustomerPersonalDataPage(PersonalDataDTO personalDataDTO, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("dataSubmitted", false);
        return templateFolder + "new_personal_data";
    }

    @PostMapping("${endpoints.tvpp.customers.path.personal_data}")
    public String postPersonalData(@Valid PersonalDataDTO personalDataDTO, BindingResult result, Model model) {
        model.addAttribute("dataSubmitted", true);
        if (result.hasErrors()) {
            return templateFolder + "new_personal_data";
        }

        if (!CommonEntityService.validateDate(personalDataDTO.getBirthday())) {
            result.addError(new FieldError("customerDTO", "birthday", notDateErrorMessage));
            return templateFolder + "new_personal_data";
        }
        sessionScopedCustomerDTO.setPersonalDataDTO(personalDataDTO);
        return "redirect:" + controllerPath + passportPath;
    }


    private void setPassportPageModel(Model model) {
        PassportType[] passportTypes = PassportType.values();
        model.addAttribute("passportTypes", passportTypes);
    }

    @GetMapping("${endpoints.tvpp.customers.path.passport}")
    public String showPassportPage(PassportDTO passportDTO, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        setPassportPageModel(model);
        model.addAttribute("dataSubmitted", false);
        return templateFolder + "new_passport_page";
    }

    @PostMapping("${endpoints.tvpp.customers.path.passport}")
    public String postPassportData(@Valid PassportDTO passportDTO, BindingResult result, Model model) {
        model.addAttribute("dataSubmitted", true);
        boolean issueDateValid = CommonEntityService.validateDate(passportDTO.getIssueDate());
        if (result.hasErrors() || !issueDateValid) {
            setPassportPageModel(model);
            if (!issueDateValid) {
                result.addError(new FieldError("passportDTO", "issueDate", notDateErrorMessage));
            }
            return templateFolder + "new_passport_page";
        }
        sessionScopedCustomerDTO.setPassportDTO(passportDTO);
        return "redirect:" + controllerPath + addressPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.address}")
    public String showAddressPage(AddressDTO addressDTO, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("dataSubmitted", false);
        return templateFolder + "new_address_page";
    }

    @PostMapping("${endpoints.tvpp.customers.path.address}")
    public String postAddressData(@Valid AddressDTO addressDTO, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("dataSubmitted", true);
            commonEntityService.setNiceValidationMessages(model, result, Map.of("houseNr", fieldNotIntMessage, "postcode", fieldNotIntMessage, "flatNr", fieldNotIntMessage), "java.lang.NumberFormatException");
            return templateFolder + "new_address_page";
        }
        sessionScopedCustomerDTO.setAddressDTO(addressDTO);
        return "redirect:" + controllerPath + loginPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.login}")
    public String showLoginDataPage(LoginDataDTO loginDataDTO, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("dataSubmitted", false);
        return templateFolder + "new_login_page";
    }

    @PostMapping("${endpoints.tvpp.customers.path.login}")
    public String postLoginData(@Valid LoginDataDTO loginDataDTO, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("dataSubmitted", true);
            return templateFolder + "new_login_page";
        }
        sessionScopedCustomerDTO.setLoginDataDTO(loginDataDTO);
        return "redirect:" + controllerPath + overviewPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.overview}")
    public String showOverviewPage(Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("customerView", new CustomerView(sessionScopedCustomerDTO));
        return templateFolder + "overview_page";
    }


    @PostMapping("${endpoints.tvpp.customers.path.overview}")
    public String saveNewCustomer(Model model) {
        action = EntityActions.CREATE;
        try {
            Customer customer = customersService.saveNewCustomer(sessionScopedCustomerDTO);
            successfulAction = true;
            successId = customer.getId();
            return "redirect:" + controllerPath;
        } catch (EntityCannotBeSavedException e) {
            commonEntityService.setEntityCannotBeSavedModel(model, e, action);
            model.addAttribute("customerView", new CustomerView(sessionScopedCustomerDTO));
            return templateFolder + "overview_page";
        }
    }


    @DeleteMapping("/{id}")
    public String deleteCustomer(@PathVariable("id") int id, Model model) {
        action = EntityActions.DELETE;
        customersService.deleteCustomer(id);
        successfulAction = true;
        successId = id;
        return "redirect:" + controllerPath;
    }

    @GetMapping("/{id}")
    public String getCustomerDetails(@PathVariable("id") int id, Model model) {
        action = EntityActions.READ;
        Customer customer = customersService.getCustomer(id);
        displayedCustomer = new CustomerView(customer);
        customersMobileNumbers = contractsService.getMobileNumbersOfCustomer(customer);
        viewDetails = true;
        return "redirect:" + controllerPath;
    }


    @GetMapping("${endpoints.tvpp.customers.path.personal_data.edit}")
    public String showUpdateForm(@PathVariable("id") long id, @RequestParam(required = false) Boolean back, Model model, PersonalDataDTO personalDataDTO) {
        model.addAttribute("processProgress", true);
        model.addAttribute("personalDataDTO", sessionScopedCustomerDTO.getPersonalDataDTO());
        CustomerView customerView = new CustomerView(customersService.getCustomer(id));
        sessionScopedCustomerView.setCustomerViewFields(customerView);
        model.addAttribute("customerView", sessionScopedCustomerView);
        return templateFolder + "edit_personal_data";
    }

    @PostMapping("${endpoints.tvpp.customers.path.personal_data.edit}")
    public String postUpdatePersonalData(@PathVariable("id") long id, @Valid PersonalDataDTO personalDataDTO, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors() || !CommonEntityService.validateDate(personalDataDTO.getBirthday())) {
            model.addAttribute("customerView", sessionScopedCustomerView);
            if (!CommonEntityService.validateDate(personalDataDTO.getBirthday())) {
                result.addError(new FieldError("customerDTO", "birthday", notDateErrorMessage));
            }
            return templateFolder + "edit_personal_data";
        }
        sessionScopedCustomerDTO.setPersonalDataDTO(personalDataDTO);
        return "redirect:" + controllerPath + passportEditPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.passport.edit}")
    public String showUpdatePassportPage(@PathVariable("id") long id, Model model, PassportDTO passportDTO) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        setPassportPageModel(model);
        model.addAttribute("passportDTO", sessionScopedCustomerDTO.getPassportDTO());
        model.addAttribute("customerView", sessionScopedCustomerView);
        return templateFolder + "edit_passport_page";
    }

    @PostMapping("${endpoints.tvpp.customers.path.passport.edit}")
    public String postUpdatePassportData(@Valid PassportDTO passportDTO, BindingResult result, Model model) {
        boolean issueDateValid = CommonEntityService.validateDate(passportDTO.getIssueDate());

        if (result.hasErrors() || !issueDateValid) {
            setPassportPageModel(model);
            model.addAttribute("customerView", sessionScopedCustomerView);
            if (!issueDateValid) {
                result.addError(new FieldError("passportDTO", "issueDate", notDateErrorMessage));
            }
            return templateFolder + "edit_passport_page";
        }
        sessionScopedCustomerDTO.setPassportDTO(passportDTO);
        return "redirect:" + controllerPath + addressEditPath;
    }


    @GetMapping("${endpoints.tvpp.customers.path.address.edit}")
    public String showUpdateAddressForm(@PathVariable("id") long id, Model model, AddressDTO addressDTO) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("customerView", sessionScopedCustomerView);
        model.addAttribute("addressDTO", sessionScopedCustomerDTO.getAddressDTO());
        return templateFolder + "edit_address_page";
    }


    @PostMapping("${endpoints.tvpp.customers.path.address.edit}")
    public String postUpdateAddressData(@PathVariable("id") long id, @Valid AddressDTO addressDTO, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("customerView", sessionScopedCustomerView);
            //todo: если валидационные сообщения заменяются в setNiceValidationMessages, то на странице после валидации поля пустые. то же самое в создании кастомера
            //todo: а тут нужна мапа, или у всех полей будут одинаковые сообщения?

            commonEntityService.setNiceValidationMessages(model, result, Map.of("houseNr", fieldNotIntMessage, "postcode", fieldNotIntMessage, "flatNr", fieldNotIntMessage), "java.lang.NumberFormatException");
            return templateFolder + "edit_address_page";
        }
        sessionScopedCustomerDTO.setAddressDTO(addressDTO);
        return "redirect:" + controllerPath + loginEditPath;
    }


    @GetMapping("${endpoints.tvpp.customers.path.login.edit}")
    public String showEditLoginDataPage(@PathVariable("id") long id, LoginDataDTO loginDataDTO, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("customerView", sessionScopedCustomerView);
        model.addAttribute("loginDataDTO", sessionScopedCustomerDTO.getLoginDataDTO());
        return templateFolder + "edit_login_page";
    }


    @PostMapping("${endpoints.tvpp.customers.path.login.edit}")
    public String postEditLoginData(@PathVariable("id") long id, @Valid LoginDataDTO loginDataDTO, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("customerView", sessionScopedCustomerView);
            return templateFolder + "edit_login_page";
        }
        sessionScopedCustomerDTO.setLoginDataDTO(loginDataDTO);
        return "redirect:" + controllerPath + overviewEditPath;
    }

    @GetMapping("${endpoints.tvpp.customers.path.overview.edit}")
    public String showOverviewPage(@PathVariable("id") long id, Model model) {
        if (!commonEntityService.isLegalProcessEntry(model)) {
            return "redirect:" + controllerPath;
        }
        model.addAttribute("customerView", new CustomerView(sessionScopedCustomerDTO));
        return templateFolder + "edit_overview_page";
    }


    @PostMapping("${endpoints.tvpp.customers.path.overview.edit}")
    public String saveUpdatedCustomer(@PathVariable("id") long id, Model model) {
        action = EntityActions.UPDATE;
        try {
            customersService.updateCustomer(id, sessionScopedCustomerDTO);
            successfulAction = true;
            successId = id;
            return "redirect:" + controllerPath;
        } catch (EntityCannotBeSavedException e) {
            commonEntityService.setEntityCannotBeSavedModel(model, e, action);
            model.addAttribute("customerView", new CustomerView(sessionScopedCustomerDTO));
            return templateFolder + "edit_overview_page";
        }
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        if (ExceptionUtils.getRootCauseMessage(e).contains("table \"contracts\"")) {
            errorMessage = customerDeleteInContractMessage;
        } else {
            errorMessage = e.getMessage();
        }
        return "redirect:" + controllerPath;
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        errorMessage = e.getMessage();
        System.out.println("errorMessage = " + errorMessage);
        return "redirect:" + controllerPath;
    }


}
