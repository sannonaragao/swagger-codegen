package io.swagger.codegen.v3.templates;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlebarTemplateEngine implements TemplateEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateEngine.class);
  private CodegenConfig config;

  public HandlebarTemplateEngine(CodegenConfig config) {
    this.config = config;
  }

  @Override
  public String getRendered(String templateFile, Map<String, Object> templateData)
      throws IOException {
//        templateData.forEach((s, o) -> {
//            LOGGER.warn("HandlebarTemplateEngine String: {} Object: {} ", s, o);
//        });

    final Object dataModels = templateData.get("models");
    if (dataModels instanceof List) {
      final List<?> dataModelsList = (List<?>) dataModels;
      for (final Object entry : dataModelsList) {
        if (entry instanceof Map) {
          //final Map<?, ?> entryMap = (Map<?, ?>) entry;
          final Map<String, CodegenModel> entryMap = (Map<String, CodegenModel>) entry;
          final Object model = entryMap.get("model");
          if (model instanceof CodegenModel) {
            CodegenModel codegenModel = (CodegenModel) model;

//          LOGGER.warn("codegenModel getRendered 43 {}", codegenModel);
//          LOGGER.warn("codegenModel getModelJson {}", codegenModel.getModelJson());
            codegenModel.getOptionalVars().forEach(s -> {
//              codegenModel.getVendorExtensions().entrySet().forEach(stringObjectEntry -> {
//                LOGGER.warn("stringObjectEntry.getKey() -> {} stringObjectEntry.getValue()-> {}",
//                    stringObjectEntry.getKey(), stringObjectEntry.getValue());
//              });
              codegenModel.getVendorExtensions().entrySet()
                  .stream()
                  .filter(map -> map.getKey().startsWith("x-prop-") && map.getKey()
                      .contains(s.getBaseName()))
                  .forEach(map -> {
                    int pos =
                        map.getKey().indexOf(s.getBaseName()) + s.getBaseName()
                            .length() + 1;
                    s.getVendorExtensions()
                        .put("x-prop-" + map.getKey().substring(pos),
                            map.getValue());
                  });
            });
          }
        }
      }
    }

    final com.github.jknack.handlebars.Template hTemplate = getHandlebars(templateFile);
    return hTemplate.apply(templateData);
  }

  @Override
  public String getName() {
    return CodegenConstants.HANDLEBARS_TEMPLATE_ENGINE;
  }

  private com.github.jknack.handlebars.Template getHandlebars(String templateFile)
      throws IOException {
    final boolean needFileTemplateLoader = StringUtils.isNotBlank(config.customTemplateDir());
    final boolean fileExist = new File(templateFile).exists();
    templateFile = templateFile.replace(".mustache", StringUtils.EMPTY).replace("\\", "/");
    final String templateDir;
    TemplateLoader templateLoader = null;
    if (needFileTemplateLoader && fileExist) {
      templateDir = config.customTemplateDir().replace("\\", "/");
      templateFile = resolveTemplateFile(templateDir, templateFile);
      templateLoader = new FileTemplateLoader(templateDir, ".mustache");
    } else {
      templateDir = config.templateDir().replace("\\", "/");
      templateFile = resolveTemplateFile(templateDir, templateFile);
      templateLoader = new ClassPathTemplateLoader("/" + templateDir, ".mustache");
    }
    final Handlebars handlebars = new Handlebars(templateLoader);
    handlebars.prettyPrint(true);
    config.addHandlebarHelpers(handlebars);
    return handlebars.compile(templateFile);
  }

  private String resolveTemplateFile(String templateDir, String templateFile) {
    if (templateFile.startsWith(templateDir)) {
      templateFile = StringUtils.replaceOnce(templateFile, templateDir, StringUtils.EMPTY);
    }
    return templateFile;
  }
}
