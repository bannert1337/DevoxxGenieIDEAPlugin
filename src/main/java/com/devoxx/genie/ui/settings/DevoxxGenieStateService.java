package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.devoxx.genie.model.Constant.*;

@Getter
@Setter
@State(
    name = "com.devoxx.genie.ui.SettingsState",
    storages = @Storage("DevoxxGenieSettingsPlugin.xml")
)
public final class DevoxxGenieStateService implements PersistentStateComponent<DevoxxGenieStateService> {

    public static DevoxxGenieStateService getInstance() {
        return ApplicationManager.getApplication().getService(DevoxxGenieStateService.class);
    }

    private List<CustomPrompt> customPrompts = new ArrayList<>();

    private List<LanguageModel> languageModels = new ArrayList<>();

    // Local LLM URL fields
    private String ollamaModelUrl = OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = LMSTUDIO_MODEL_URL;
    private String gpt4allModelUrl = GPT4ALL_MODEL_URL;
    private String janModelUrl = JAN_MODEL_URL;
    private String exoModelUrl = EXO_MODEL_URL;
    private String llamaCPPUrl = LLAMA_CPP_MODEL_URL;

    // LLM API Keys
    private String openAIKey = "";
    private String mistralKey = "";
    private String anthropicKey = "";
    private String groqKey = "";
    private String deepInfraKey = "";
    private String geminiKey = "";

    // Search API Keys
    private Boolean hideSearchButtonsFlag = HIDE_SEARCH_BUTTONS;
    private String googleSearchKey = "";
    private String googleCSIKey = "";
    private String tavilySearchKey = "";
    private Integer maxSearchResults = MAX_SEARCH_RESULTS;

    // Last selected language model
    @Getter
    @Setter
    private String selectedProvider;
    private String selectedLanguageModel;

    // Enable stream mode
    private Boolean streamMode = STREAM_MODE;

    // LLM settings
    private Double temperature = TEMPERATURE;
    private Double topP = TOP_P;

    private Integer timeout = TIMEOUT;
    private Integer maxRetries = MAX_RETRIES;
    private Integer chatMemorySize = MAX_MEMORY;
    private Integer maxOutputTokens = MAX_OUTPUT_TOKENS;

    // Enable AST mode
    private Boolean astMode = AST_MODE;
    private Boolean astParentClass = AST_PARENT_CLASS;
    private Boolean astClassReference = AST_CLASS_REFERENCE;
    private Boolean astFieldReference = AST_FIELD_REFERENCE;

    private String systemPrompt = SYSTEM_PROMPT;
    private String testPrompt = TEST_PROMPT;
    private String reviewPrompt = REVIEW_PROMPT;
    private String explainPrompt = EXPLAIN_PROMPT;

    private Boolean excludeJavaDoc = false;

    private List<String> excludedDirectories = new ArrayList<>(Arrays.asList(
        "build", ".git", "bin", "out", "target", "node_modules", ".idea"
    ));

    private List<String> includedFileExtensions = new ArrayList<>(Arrays.asList(
        "java", "kt", "groovy", "scala", "xml", "json", "yaml", "yml", "properties", "txt", "md"
    ));

    private Map<String, Double> modelInputCosts = new HashMap<>();
    private Map<String, Double> modelOutputCosts = new HashMap<>();

    private Map<String, Integer> modelWindowContexts = new HashMap<>();
    private Integer defaultWindowContext = 8000;

    public DevoxxGenieStateService() {
        initializeDefaultPrompts();
    }

    private void initializeDefaultPrompts() {
        if (customPrompts.isEmpty()) {
            customPrompts.add(new CustomPrompt("test", TEST_PROMPT));
            customPrompts.add(new CustomPrompt("explain", EXPLAIN_PROMPT));
            customPrompts.add(new CustomPrompt("review", REVIEW_PROMPT));
        }
    }

    @Override
    public DevoxxGenieStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DevoxxGenieStateService state) {
        XmlSerializerUtil.copyBean(state, this);
        initializeDefaultCostsIfEmpty();
        initializeDefaultPrompts();
    }

    public void setModelCost(ModelProvider provider,
                             String modelName,
                             double inputCost,
                             double outputCost) {
        if (DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
            String key = provider.getName() + ":" + modelName;
            modelInputCosts.put(key, inputCost);
            modelOutputCosts.put(key, outputCost);
        }
    }

//    public double getModelInputCost(ModelProvider provider, String modelName) {
//        if (DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
//            String key = provider.getName() + ":" + modelName;
//            return modelInputCosts.getOrDefault(key,
//                DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.getOrDefault(new DefaultLLMSettingsUtil.CostKey(provider, modelName), 0.0));
//        }
//        return 0.0;
//    }

    public double getModelInputCost(@NotNull ModelProvider provider, String modelName) {
        String key = provider.getName() + ":" + modelName;
        double cost = modelInputCosts.getOrDefault(key, 0.0);
        if (cost == 0.0) {
            DefaultLLMSettingsUtil.CostKey costKey = new DefaultLLMSettingsUtil.CostKey(provider, modelName);
            cost = DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.getOrDefault(costKey, 0.0);
            if (cost == 0.0) {
                // Fallback to similar model names
                for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.entrySet()) {
                    if (entry.getKey().provider == provider && entry.getKey().modelName.startsWith(modelName.split("-")[0])) {
                        cost = entry.getValue();
                        break;
                    }
                }
            }
        }
        return cost;
    }

    public double getModelOutputCost(ModelProvider provider, String modelName) {
        if (DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
            String key = provider.getName() + ":" + modelName;
            return modelOutputCosts.getOrDefault(key,
                DefaultLLMSettingsUtil.DEFAULT_OUTPUT_COSTS.getOrDefault(new DefaultLLMSettingsUtil.CostKey(provider, modelName), 0.0));
        }
        return 0.0;
    }

    private void initializeDefaultCostsIfEmpty() {
        if (modelInputCosts.isEmpty()) {
            for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.entrySet()) {
                String key = entry.getKey().provider.getName() + ":" + entry.getKey().modelName;
                modelInputCosts.put(key, entry.getValue());
            }
        }
        if (modelOutputCosts.isEmpty()) {
            for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_OUTPUT_COSTS.entrySet()) {
                String key = entry.getKey().provider.getName() + ":" + entry.getKey().modelName;
                modelOutputCosts.put(key, entry.getValue());
            }
        }
    }

    public void setModelWindowContext(ModelProvider provider, String modelName, int windowContext) {
        if (DefaultLLMSettingsUtil.isApiBasedProvider(provider)) {
            String key = provider.getName() + ":" + modelName;
            modelWindowContexts.put(key, windowContext);
        }
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull List<LanguageModel> getLanguageModels() {
        return new ArrayList<>(languageModels);
    }

    public void setLanguageModels(List<LanguageModel> models) {
        this.languageModels = new ArrayList<>(models);
    }

}
