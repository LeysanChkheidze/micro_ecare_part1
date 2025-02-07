package com.javaschool.microecare.controllers.tvpp;

import com.javaschool.microecare.catalogmanagement.dao.Option;
import com.javaschool.microecare.catalogmanagement.dto.OptionDTO;
import com.javaschool.microecare.catalogmanagement.service.OptionsService;
import com.javaschool.microecare.catalogmanagement.viewmodel.OptionView;
import com.javaschool.microecare.commonentitymanagement.service.CommonEntityService;
import com.javaschool.microecare.commonentitymanagement.dao.EntityCannotBeSavedException;
import com.javaschool.microecare.contractmanagement.service.ContractsService;
import com.javaschool.microecare.utils.EntityActions;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Map;

/**
 * Controller Options page in TVPP.
 */
@Controller
@RequestMapping("${endpoints.tvpp.options.controller_path}")
@PropertySource("messages.properties")
public class OptionsPageTVPPController {
    @Value("${directory.templates.tvpp.options}")
    private String templateFolder;
    @Value("${endpoints.tvpp.options.controller_path}")
    private String controllerPath;
    @Value("${general.price.nonnumber.msg}")
    private String priceDigitsMessage;
    @Value("${option.delete.in_contract.msg}")
    private String optionDeleteInContractMessage;
    @Value("${endpoints.tvpp.entity.path.new}")
    private String newOptionPath;

    private final String ENTITY_NAME = "Option";

    private boolean successfulAction = false;
    private long successId;
    private boolean viewDetails = false;
    private OptionView displayedOption;
    private int numberOfContractsWithOption;
    private String errorMessage;
    private EntityActions action;

    final CommonEntityService commonEntityService;
    final OptionsService optionsService;
    final ContractsService contractsService;

    //TODO: сделать везде лейаут дропдаунов так же, как в редактировании кастомера в паспортных данных

    /**
     * Instantiates a new Options page tvpp controller.
     *
     * @param commonEntityService the CommonEntityService service with methods relevant to any entity
     * @param optionsService      the OptionsService
     */
    public OptionsPageTVPPController(CommonEntityService commonEntityService, OptionsService optionsService, ContractsService contractsService) {
        this.commonEntityService = commonEntityService;
        this.optionsService = optionsService;
        this.contractsService = contractsService;
    }

    /**
     * Sets paths attributes for paths which are standard for CRUD operations for any entity.
     *
     * @param model the model of the page
     */
    @ModelAttribute
    public void setPathsAttributes(Model model) {
        commonEntityService.setPathsAttributes(model, controllerPath);
    }

    /**
     * Sets list of all found option views and attributes to display confirmation modal window into model
     *
     * @param model the model of the page
     */
    private void setAllOptionsModel(Model model) {
        model.addAttribute("options", optionsService.getAllOptionViews());
        if (successfulAction) {
            commonEntityService.setSuccessfulActionModel(model, ENTITY_NAME, action, successId);
        }
        if (viewDetails) {
            model.addAttribute("viewOptionDetails", true);
            model.addAttribute("displayedOption", displayedOption);
            model.addAttribute("numberOfContractsWithOption", numberOfContractsWithOption);
            viewDetails = false;
        }
        if (errorMessage != null) {
            commonEntityService.setErrorModel(model, ENTITY_NAME, errorMessage, action);
        }

    }

    /**
     * Returns all options page with required model attributes at get request.
     * Sets successfulAction to false after the actual value of the field was set into model in setAllOptionsModel method
     *
     * @param model the model
     * @return all options page template
     */
    @GetMapping
    public String getOptionsPage(Model model) {
        setAllOptionsModel(model);
        successfulAction = false;
        errorMessage = null;
        action = null;
        viewDetails = false;
        return templateFolder + "options";
    }

    /**
     * Returns new option page at get request.
     *
     * @param optionDTO the option dto which will be used to create a new option
     * @param model     the model of the page
     * @return new option page template
     */
    @GetMapping("${endpoints.tvpp.entity.path.new}")
    public String showNewOptionPage(OptionDTO optionDTO, BindingResult result, Model model) {
        return templateFolder + "new_option";
    }

    /**
     * Creates new option at post request using validated OptionDTO.
     * In case of validation errors in OptionDTO returns new option page with human-readable validation messages in model
     * In case if EntityCannotBeSavedException caught during saving new option returns new option page with
     * error field name and error message in model
     *
     * @param optionDTO the option dto to create new option
     * @param result    binding result
     * @param model     page model
     * @return all options or new option page template depending on result of saving of the new option
     */
    @PostMapping
    public String createNewOption(@Valid OptionDTO optionDTO, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        //todo: если фейлится валидация на странице, то все поля пустые. а в редактировании ок
        action = EntityActions.CREATE;
        if (result.hasErrors()) {
            model.addAttribute("errorAction", action.getText());
            commonEntityService.setNiceValidationMessages(model, result, Map.of("monthlyPrice", priceDigitsMessage, "oneTimePrice", priceDigitsMessage), "java.lang.NumberFormatException");

            //todo: сделать, чтобы после ошибки валидации оставался урл страницы создания опции
            //вот тут ошибки не передаются в резалте:
            /*BindingResult newResult = new BeanPropertyBindingResult(result.getTarget(), result.getObjectName());

            for (FieldError error : result.getFieldErrors()) {
                newResult.addError(error);
            }
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.optionDTO", newResult);
            redirectAttributes.addFlashAttribute("kokoko", "kekeke");
            System.out.println();*/
           // return "redirect:" + controllerPath + newOptionPath;
             return templateFolder + "new_option";
        }
        try {
            Option newOption = optionsService.saveNewOption(optionDTO);
            successfulAction = true;
            successId = newOption.getId();
            return "redirect:" + controllerPath;
        } catch (EntityCannotBeSavedException e) {
            commonEntityService.setEntityCannotBeSavedModel(model, e, action);
            return templateFolder + "new_option";
        }

    }

    /**
     * Returns update option page at get request.
     *
     * @param id    the id of the option to update
     * @param model the page model
     * @return update option page template
     */
    @GetMapping("${endpoints.tvpp.entity.path.edit}")
    public String showUpdateForm(@PathVariable("id") int id, Model model) {
        Option option = optionsService.getOption(id);
        OptionDTO optionDTO = new OptionDTO(option);
        OptionView optionView = new OptionView(option);
        model.addAttribute("optionDTO", optionDTO);
        model.addAttribute("optionView", optionView);
        return templateFolder + "edit_option";
    }

    /**
     * Updates existing option at patch request using validated OptionDTO.
     * In case of validation errors in OptionDTO returns update option page with human-readable validation messages in model
     * In case if EntityCannotBeSavedException caught during saving updated option returns update option page with
     * error field name and error message in model
     *
     * @param id        the id of the option to update
     * @param optionDTO the option dto to use to set new parameters of the option
     * @param result    the binding result
     * @param model     the page model
     * @return all options or update option template depending on result of saving of the new option
     */
    @PatchMapping("/{id}")
    public String updateOption(@PathVariable("id") long id, @Valid OptionDTO optionDTO,
                               BindingResult result, Model model) {
        action = EntityActions.UPDATE;
        if (result.hasErrors()) {
            model.addAttribute("errorAction", action.getText());
            commonEntityService.setNiceValidationMessages(model, result, Map.of("monthlyPrice", priceDigitsMessage, "oneTimePrice", priceDigitsMessage), "java.lang.NumberFormatException");
            OptionView optionView = new OptionView(optionsService.getOption(id));
            model.addAttribute("optionView", optionView);
            return templateFolder + "edit_option";
        }

        try {
            Option updatedOption = optionsService.updateOption(id, optionDTO);
            successfulAction = true;
            successId = updatedOption.getId();
            return "redirect:" + controllerPath;
        } catch (EntityCannotBeSavedException e) {
            commonEntityService.setEntityCannotBeSavedModel(model, e, action);
            OptionView optionView = new OptionView(optionsService.getOption(id));
            model.addAttribute("optionView", optionView);
            return templateFolder + "edit_option";
        }
    }

    /**
     * Deletes existing option at delete request. Removes tariff-option relationship for deleted option
     *
     * @param id    the id of option to delete
     * @param model the model
     * @return all options page template
     */
    @DeleteMapping("/{id}")
    public String deleteTariff(@PathVariable("id") int id, Model model) {
        action = EntityActions.DELETE;
        optionsService.deleteOption(id);
        successfulAction = true;
        successId = id;
        return "redirect:" + controllerPath;
    }

    @GetMapping("/{id}")
    public String getOptionDetails(@PathVariable("id") int id, Model model) {
        action = EntityActions.READ;
        Option option = optionsService.getOption(id);
        displayedOption = new OptionView(option);
        numberOfContractsWithOption = contractsService.getNumberOfContractsWithOption(option);
        viewDetails = true;
        //todo: do i need redirect here?
        // https://stackoverflow.com/questions/68949567/pass-data-from-spring-boot-controller-to-boostrap-modal
        return "redirect:" + controllerPath;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        if (ExceptionUtils.getRootCauseMessage(e).contains("table \"contract_option\"")) {
            errorMessage = optionDeleteInContractMessage;
        } else {
            errorMessage = e.getMessage();
        }
        return "redirect:" + controllerPath;
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e) {
        errorMessage = e.getMessage();
        return "redirect:" + controllerPath;
    }
}
