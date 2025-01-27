/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import com.google.common.base.Strings;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openapitools.codegen.utils.StringUtils.*;

public class ElixirClientCodegen extends DefaultCodegen {
    private final Logger LOGGER = LoggerFactory.getLogger(ElixirClientCodegen.class);

    private final Pattern simpleAtomPattern = Pattern.compile("\\A(?:(?:[_@\\p{Alpha}][_@\\p{Alnum}]*[?!]?)|-)\\z");

    protected String apiVersion = "1.0.0";
    protected String moduleName;
    protected static final String defaultModuleName = "OpenAPI.Client";

    // This is the name of elixir project name;
    protected static final String defaultPackageName = "openapi_client";

    String supportedElixirVersion = "1.10";
    List<String> extraApplications = Arrays.asList(":logger");
    List<String> deps = Arrays.asList(
            "{:tesla, \"~> 1.7\"}",
            "{:jason, \"~> 1.4\"}",
            "{:ex_doc, \"~> 0.28\", only: :dev, runtime: false}"
    );

    protected List<String> charactersToAllow = Collections.singletonList("_");

    protected Set<String> keywordsThatDoNotSupportRawIdentifiers = new HashSet<>(
            Arrays.asList());

    public ElixirClientCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .securityFeatures(EnumSet.of(
                        SecurityFeature.OAuth2_Implicit,
                        SecurityFeature.BasicAuth
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie
                )
                .includeClientModificationFeatures(
                        ClientModificationFeature.BasePath
                )
                .includeDataTypeFeatures(
                        DataTypeFeature.AnyType
                )
        );

        // set the output folder here
        outputFolder = "generated-code/elixir";

        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put(
                "model.mustache", // the template to use
                ".ex");       // the extension for each file to write

        /**
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                ".ex");       // the extension for each file to write

        /**
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "elixir";

        /**
         * Reserved words.  Override this with reserved words specific to your language
         * Ref: https://github.com/itsgreggreg/elixir_quick_reference#reserved-words
         */
        reservedWords = new HashSet<>(
                Arrays.asList(
                        "nil",
                        "true",
                        "false",
                        "__MODULE__",
                        "__FILE__",
                        "__DIR__",
                        "__ENV__",
                        "__CALLER__")
        );

        /**
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);

        /**
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        supportingFiles.add(new SupportingFile("README.md.mustache",   // the input template or file
                "",                                                       // the destination folder, relative `outputFolder`
                "README.md")                                          // the output file
        );
        supportingFiles.add(new SupportingFile("config.exs.mustache",
                "config",
                "config.exs")
        );
        supportingFiles.add(new SupportingFile("runtime.exs.mustache",
                "config",
                "runtime.exs")
        );
        supportingFiles.add(new SupportingFile("mix.exs.mustache",
                "",
                "mix.exs")
        );
        supportingFiles.add(new SupportingFile("formatter.exs",
                "",
                ".formatter.exs")
        );
        supportingFiles.add(new SupportingFile("test_helper.exs.mustache",
                "test",
                "test_helper.exs")
        );
        supportingFiles.add(new SupportingFile("gitignore.mustache",
                "",
                ".gitignore")
        );

        /**
         * Language Specific Primitives.  These types will not trigger imports by
         * the client generator
         */
        languageSpecificPrimitives = new HashSet<>(
                Arrays.asList(
                        "Integer",
                        "Float",
                        "Decimal",
                        "Boolean",
                        "String",
                        "List",
                        "Atom",
                        "Map",
                        "AnyType",
                        "Tuple",
                        "PID",
                        "DateTime",
                        "map()", // This is a workaround, since the DefaultCodeGen uses our elixir TypeSpec datetype to evaluate the primitive
                        "any()"
                )
        );

        // ref: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#data-types
        typeMapping = new HashMap<>();
        typeMapping.put("integer", "Integer");
        typeMapping.put("long", "Integer");
        typeMapping.put("number", "Float");
        typeMapping.put("float", "Float");
        typeMapping.put("double", "Float");
        typeMapping.put("string", "String");
        typeMapping.put("byte", "Integer");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("Date", "DateTime");
        typeMapping.put("DateTime", "DateTime");
        typeMapping.put("file", "String");
        typeMapping.put("map", "Map");
        typeMapping.put("array", "List");
        typeMapping.put("list", "List");
        typeMapping.put("object", "Map");
        typeMapping.put("binary", "String");
        typeMapping.put("ByteArray", "String");
        typeMapping.put("UUID", "String");
        typeMapping.put("URI", "String");

        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, "The main namespace to use for all classes. e.g. Yay.Pets"));
        cliOptions.add(new CliOption("licenseHeader", "The license header to prepend to the top of all source files."));
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "Elixir package name (convention: lowercase)."));
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see org.openapitools.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -g flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "elixir";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates an elixir client library (alpha).";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        additionalProperties.put("supportedElixirVersion", supportedElixirVersion);
        additionalProperties.put("extraApplications", join(",", extraApplications));
        additionalProperties.put("deps", deps);
        additionalProperties.put("underscored", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                writer.write(underscored(fragment.execute()));
            }
        });
        additionalProperties.put("modulized", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                writer.write(modulized(fragment.execute()));
            }
        });
        additionalProperties.put("atom", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                writer.write(atomized(fragment.execute()));
            }
        });
        additionalProperties.put("env_var", new Mustache.Lambda() {
            @Override
            public void execute(Template.Fragment fragment, Writer writer) throws IOException {
                String text = underscored(fragment.execute());
                writer.write(text.toUpperCase(Locale.ROOT));
            }
        });

        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            setModuleName((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        }
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        Info info = openAPI.getInfo();
        if (moduleName == null) {
            if (info.getTitle() != null) {
                // default to the appName (from title field)
                setModuleName(modulized(escapeText(info.getTitle())));
            } else {
                setModuleName(defaultModuleName);
            }
        }
        additionalProperties.put("moduleName", moduleName);

        if (!additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            additionalProperties.put(CodegenConstants.PACKAGE_NAME, underscored(moduleName));
        }

        supportingFiles.add(new SupportingFile("connection.ex.mustache",
                sourceFolder(),
                "connection.ex"));

        supportingFiles.add(new SupportingFile("request_builder.ex.mustache",
                sourceFolder(),
                "request_builder.ex"));


        supportingFiles.add(new SupportingFile("deserializer.ex.mustache",
                sourceFolder(),
                "deserializer.ex"));
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationMap operations = super.postProcessOperationsWithModels(objs, allModels).getOperations();
        List<CodegenOperation> os = operations.getOperation();
        List<ExtendedCodegenOperation> newOs = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{([^\\}]+)\\}([^\\{]*)");
        for (CodegenOperation o : os) {
            ArrayList<String> pathTemplateNames = new ArrayList<>();
            Matcher matcher = pattern.matcher(o.path);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String pathTemplateName = matcher.group(1);
                matcher.appendReplacement(buffer, "#{" + underscore(pathTemplateName) + "}" + "$2");
                pathTemplateNames.add(pathTemplateName);
            }
            ExtendedCodegenOperation eco = new ExtendedCodegenOperation(o);
            if (buffer.toString().isEmpty()) {
                eco.setReplacedPathName(o.path);
            } else {
                eco.setReplacedPathName(buffer.toString());
            }
            eco.setPathTemplateNames(pathTemplateNames);

            // detect multipart form types
            if (eco.hasConsumes == Boolean.TRUE) {
                Map<String, String> firstType = eco.consumes.get(0);
                if (firstType != null) {
                    if ("multipart/form-data".equals(firstType.get("mediaType"))) {
                        eco.isMultipart = Boolean.TRUE;
                    }
                }
            }

            newOs.add(eco);
        }
        operations.setOperation(newOs);
        return objs;
    }

    @Override
    public CodegenModel fromModel(String name, Schema model) {
        CodegenModel cm = super.fromModel(name, model);
        return new ExtendedCodegenModel(cm);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse resp) {
        return new ExtendedCodegenResponse(super.fromResponse(responseCode, resp));
    }

    // We should use String.join if we can use Java8
    String join(CharSequence charSequence, Iterable<String> iterable) {
        StringBuilder buf = new StringBuilder();
        for (String str : iterable) {
            if (0 < buf.length()) {
                buf.append((charSequence));
            }
            buf.append(str);
        }
        return buf.toString();
    }

    private String underscored(String words) {
        ArrayList<String> underscoredWords = new ArrayList<>();
        for (String word : words.split(" ")) {
            underscoredWords.add(underscore(word));
        }
        return join("_", underscoredWords);
    }

    private String modulized(String words) {
        ArrayList<String> modulizedWords = new ArrayList<>();
        for (String word : words.split(" ")) {
            modulizedWords.add(camelize(word));
        }
        return join("", modulizedWords);
    }

    private String atomized(String text) {
      StringBuilder atom = new StringBuilder();
      Matcher m = simpleAtomPattern.matcher(text);

      atom.append(":");

      if (!m.matches()) {
        atom.append("\"");
      }

      atom.append(text);

      if (!m.matches()) {
        atom.append("\"");
      }

      return atom.toString();
    }


    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;  // add an underscore to the name
    }

    private String sourceFolder() {
        ArrayList<String> underscoredWords = new ArrayList<>();
        for (String word : moduleName.split("\\.")) {
            underscoredWords.add(underscore(word));
        }
        return ("lib/" + join("/", underscoredWords)).replace('/', File.separatorChar);
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + sourceFolder() + File.separator + "model";
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + sourceFolder() + File.separator + "api";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "Default";
        }
        return camelize(name);
    }

    @Override
    public String toApiFilename(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_");

        // e.g. PetApi.go => pet_api.go
        return underscore(name);
    }

    @Override
    public String toModelName(String name) {
        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(toModelFilename(name));
    }

    @Override
    public String toModelFilename(String name) {
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix;
        }

        name = sanitizeName(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", name, "model_" + name);
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name,
                    "model_" + name);
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        return underscore(name);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty (should not occur as an auto-generated method name will be used)
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            LOGGER.warn("{} (reserved word) cannot be used as method name. Renamed to {}", operationId, underscore(sanitizeName("call_" + operationId)));
            return underscore(sanitizeName("call_" + operationId));
        }

        // operationId starts with a number
        if (operationId.matches("^\\d.*")) {
            LOGGER.warn("{} (starting with a number) cannot be used as method name. Renamed to {}", operationId, underscore(sanitizeName("call_" + operationId)));
            operationId = "call_" + operationId;
        }

        return underscore(sanitizeName(operationId));
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isArraySchema(p)) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return "[" + getTypeDeclaration(inner) + "]";
        } else if (ModelUtils.isMapSchema(p)) {
            Schema inner = ModelUtils.getAdditionalProperties(p);
            return "%{optional(String.t) => " + getTypeDeclaration(inner) + "}";
        } else if (ModelUtils.isPasswordSchema(p)) {
            return "String.t";
        } else if (ModelUtils.isEmailSchema(p)) {
            return "String.t";
        } else if (ModelUtils.isByteArraySchema(p)) {
            return "binary()";
        } else if (ModelUtils.isUUIDSchema(p)) {
            return "String.t";
        } else if (ModelUtils.isDateSchema(p)) {
            return "Date.t";
        } else if (ModelUtils.isDateTimeSchema(p)) {
            return "DateTime.t";
        } else if (ModelUtils.isObjectSchema(p)) {
            return "map()";
        } else if (ModelUtils.isIntegerSchema(p)) {
            return "integer()";
        } else if (ModelUtils.isNumberSchema(p)) {
            return "float()";
        } else if (ModelUtils.isBinarySchema(p) || ModelUtils.isFileSchema(p)) {
            return "String.t";
        } else if (ModelUtils.isBooleanSchema(p)) {
            return "boolean()";
        } else if (!StringUtils.isEmpty(p.get$ref())) {
            return this.moduleName + ".Model." + super.getTypeDeclaration(p) + ".t";
        } else if (ModelUtils.isFileSchema(p)) {
            return "String.t";
        } else if (ModelUtils.isStringSchema(p)) {
            return "String.t";
        } else if (p.getType() == null) {
            return "any()";
        }
        return super.getTypeDeclaration(p);
    }

    /**
     * Optional - OpenAPI type conversion.  This is used to map OpenAPI types in a `Schema` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     */
    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(openAPIType)) {
            type = typeMapping.get(openAPIType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = openAPIType;
        return toModelName(type);
    }

    class ExtendedCodegenResponse extends CodegenResponse {
        public boolean isDefinedDefault;

        public ExtendedCodegenResponse(CodegenResponse o) {
            super();

            this.headers.addAll(o.headers);
            this.code = o.code;
            this.message = o.message;
            this.examples = o.examples;
            this.dataType = o.dataType;
            this.baseType = o.baseType;
            this.containerType = o.containerType;
            this.hasHeaders = o.hasHeaders;
            this.isString = o.isString;
            this.isNumeric = o.isNumeric;
            this.isInteger = o.isInteger;
            this.isLong = o.isLong;
            this.isNumber = o.isNumber;
            this.isFloat = o.isFloat;
            this.isDouble = o.isDouble;
            this.isByteArray = o.isByteArray;
            this.isBoolean = o.isBoolean;
            this.isDate = o.isDate;
            this.isDateTime = o.isDateTime;
            this.isUuid = o.isUuid;
            this.isEmail = o.isEmail;
            this.isModel = o.isModel;
            this.isFreeFormObject = o.isFreeFormObject;
            this.isDefault = o.isDefault;
            this.simpleType = o.simpleType;
            this.primitiveType = o.primitiveType;
            this.isMap = o.isMap;
            this.isArray = o.isArray;
            this.isBinary = o.isBinary;
            this.isFile = o.isFile;
            this.schema = o.schema;
            this.jsonSchema = o.jsonSchema;
            this.vendorExtensions = o.vendorExtensions;

            this.isDefinedDefault = (this.code.equals("0") || this.code.equals("default"));
        }

        public String codeMappingKey() {
            if (this.isDefinedDefault) {
                return ":default";
            }

            if (code.matches("^\\d{3}$")) {
                return code;
            }

            LOGGER.warn("Unknown HTTP status code: {}", this.code);
            return "\"" + code + "\"";
        }

        public String decodedStruct() {
            // Let Poison decode the entire response into a generic blob
            if (isMap) {
                return "%{}";
            }
            // Primitive return type, don't even try to decode
            if (baseType == null || (containerType == null && primitiveType)) {
                return "false";
            } else if (isArray && languageSpecificPrimitives().contains(baseType)) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            if (isArray) {
                sb.append("[");
            }
            sb.append("%");
            sb.append(moduleName);
            sb.append(".Model.");
            sb.append(baseType);
            sb.append("{}");
            if (isArray) {
                sb.append("]");
            }
            return sb.toString();
        }

    }

    class ExtendedCodegenOperation extends CodegenOperation {
        private List<String> pathTemplateNames = new ArrayList<>();
        private String replacedPathName;

        public ExtendedCodegenOperation(CodegenOperation o) {
            super();

            // Copy all fields of CodegenOperation
            this.responseHeaders.addAll(o.responseHeaders);
            this.hasAuthMethods = o.hasAuthMethods;
            this.hasConsumes = o.hasConsumes;
            this.hasProduces = o.hasProduces;
            this.hasParams = o.hasParams;
            this.hasOptionalParams = o.hasOptionalParams;
            this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
            this.returnSimpleType = o.returnSimpleType;
            this.subresourceOperation = o.subresourceOperation;
            this.isMap = o.isMap;
            this.isArray = o.isArray;
            this.isMultipart = o.isMultipart;
            this.isResponseBinary = o.isResponseBinary;
            this.hasReference = o.hasReference;
            this.isRestfulIndex = o.isRestfulIndex;
            this.isRestfulShow = o.isRestfulShow;
            this.isRestfulCreate = o.isRestfulCreate;
            this.isRestfulUpdate = o.isRestfulUpdate;
            this.isRestfulDestroy = o.isRestfulDestroy;
            this.isRestful = o.isRestful;
            this.path = o.path;
            this.operationId = o.operationId;
            this.returnType = o.returnType;
            this.httpMethod = o.httpMethod;
            this.returnBaseType = o.returnBaseType;
            this.returnContainer = o.returnContainer;
            this.summary = o.summary;
            this.unescapedNotes = o.unescapedNotes;
            this.notes = o.notes;
            this.baseName = o.baseName;
            this.defaultResponse = o.defaultResponse;
            this.discriminator = o.discriminator;
            this.consumes = o.consumes;
            this.produces = o.produces;
            this.bodyParam = o.bodyParam;
            this.allParams = o.allParams;
            this.bodyParams = o.bodyParams;
            this.pathParams = o.pathParams;
            this.queryParams = o.queryParams;
            this.headerParams = o.headerParams;
            this.formParams = o.formParams;
            this.requiredParams = o.requiredParams;
            this.optionalParams = o.optionalParams;
            this.authMethods = o.authMethods;
            this.tags = o.tags;
            this.responses = o.responses;
            this.imports = o.imports;
            this.examples = o.examples;
            this.externalDocs = o.externalDocs;
            this.vendorExtensions = o.vendorExtensions;
            this.nickname = o.nickname;
            this.operationIdLowerCase = o.operationIdLowerCase;
            this.operationIdCamelCase = o.operationIdCamelCase;
        }

        public List<String> getPathTemplateNames() {
            return pathTemplateNames;
        }

        public void setPathTemplateNames(List<String> pathTemplateNames) {
            this.pathTemplateNames = pathTemplateNames;
        }

        public String getReplacedPathName() {
            return replacedPathName;
        }

        public void setReplacedPathName(String replacedPathName) {
            this.replacedPathName = replacedPathName;
        }

        public String typespec() {
            StringBuilder sb = new StringBuilder("@spec ");
            sb.append(underscore(operationId));
            sb.append("(Tesla.Env.client, ");

            for (CodegenParameter param : allParams) {
                if (param.required) {
                    buildTypespec(param, sb);
                    sb.append(", ");
                }
            }

            sb.append("keyword()) :: ");
            HashSet<String> uniqueResponseTypes = new HashSet<>();
            for (CodegenResponse response : this.responses) {
                ExtendedCodegenResponse exResponse = (ExtendedCodegenResponse) response;
                StringBuilder returnEntry = new StringBuilder();
                if (exResponse.baseType == null) {
                    returnEntry.append("nil");
                } else if (exResponse.containerType == null) { // not container (array, map, set)
                    if (!exResponse.primitiveType) {
                        returnEntry.append(moduleName);
                        returnEntry.append(".Model.");
                    }

                    if (exResponse.baseType.equals("AnyType")) {
                        returnEntry.append("any()");
                    }else {
                        returnEntry.append(exResponse.baseType);
                        returnEntry.append(".t");
                    }
                } else {
                    if (exResponse.containerType.equals("array") ||
                            exResponse.containerType.equals("set")) {
                        returnEntry.append("list(");
                        if (!exResponse.primitiveType) {
                            returnEntry.append(moduleName);
                            returnEntry.append(".Model.");
                        }

                        if (exResponse.baseType.equals("AnyType")) {
                            returnEntry.append("any())");
                        }else {
                            returnEntry.append(exResponse.baseType);
                            returnEntry.append(".t)");
                        }
                    } else if (exResponse.containerType.equals("map")) {
                        returnEntry.append("map()");
                    }
                }
                uniqueResponseTypes.add(returnEntry.toString());
            }

            for (String returnType : uniqueResponseTypes) {
                sb.append("{:ok, ").append(returnType).append("} | ");
            }

            sb.append("{:error, Tesla.Env.t}");
            return sb.toString();
        }

        private void buildTypespec(CodegenParameter param, StringBuilder sb) {
            if (param.dataType == null) {
                sb.append("nil");
            } else if (param.isArray) {
                // list(<subtype>)
                sb.append("list(");
                buildTypespec(param.items, sb);
                sb.append(")");
            } else if (param.isMap) {
                // %{optional(String.t) => <subtype>}
                sb.append("%{optional(String.t) => ");
                buildTypespec(param.items, sb);
                sb.append("}");
            } else if (param.isPrimitiveType) {
                // like `integer()`, `String.t`
                sb.append(param.dataType);
            } else if (param.isFile || param.isBinary) {
                sb.append("String.t");
            } else if ("String.t".equals(param.dataType)) {
                // uuid, password, etc
                sb.append(param.dataType);
            } else {
                // <module>.Model.<type>.t
                sb.append(moduleName);
                sb.append(".Model.");
                sb.append(param.dataType);
                sb.append(".t");
            }
        }

        private void buildTypespec(CodegenProperty property, StringBuilder sb) {
            if (property == null) {
                LOGGER.error("CodegenProperty cannot be null. Please report the issue to https://github.com/openapitools/openapi-generator with the spec");
            } else if (property.isArray) {
                sb.append("list(");
                buildTypespec(property.items, sb);
                sb.append(")");
            } else if (property.isMap) {
                sb.append("%{optional(String.t) => ");
                buildTypespec(property.items, sb);
                sb.append("}");
            } else if (property.isPrimitiveType) {
                sb.append(property.baseType);
                sb.append(".t");
            } else {
                sb.append(moduleName);
                sb.append(".Model.");
                sb.append(property.baseType);
                sb.append(".t");
            }
        }

        private boolean getRequiresHttpcWorkaround() {
            // Only POST/PATCH/PUT are affected from the httpc bug
            if (!(this.httpMethod.equals("POST") || this.httpMethod.equals("PATCH") || this.httpMethod.equals("PUT"))) {
                return false;
            }

            // If theres something required for the body, the workaround is not required
            for (CodegenParameter requiredParam : this.requiredParams) {
                if (requiredParam.isBodyParam || requiredParam.isFormParam) {
                    return false;
                }
            }

            // In case there is nothing for the body, the operation requires the workaround
            return true;
        }
    }

    class ExtendedCodegenModel extends CodegenModel {
        public boolean hasImports;

        public ExtendedCodegenModel(CodegenModel cm) {
            super();

            // Copy all fields of CodegenModel
            this.parent = cm.parent;
            this.parentSchema = cm.parentSchema;
            this.parentModel = cm.parentModel;
            this.interfaceModels = cm.interfaceModels;
            this.children = cm.children;
            this.name = cm.name;
            this.classname = cm.classname;
            this.title = cm.title;
            this.description = cm.description;
            this.classVarName = cm.classVarName;
            this.modelJson = cm.modelJson;
            this.dataType = cm.dataType;
            this.xmlPrefix = cm.xmlPrefix;
            this.xmlNamespace = cm.xmlNamespace;
            this.xmlName = cm.xmlName;
            this.classFilename = cm.classFilename;
            this.unescapedDescription = cm.unescapedDescription;
            this.discriminator = cm.discriminator;
            this.defaultValue = cm.defaultValue;
            this.arrayModelType = cm.arrayModelType;
            this.isAlias = cm.isAlias;
            this.vars = cm.vars;
            this.requiredVars = cm.requiredVars;
            this.optionalVars = cm.optionalVars;
            this.readOnlyVars = cm.readOnlyVars;
            this.readWriteVars = cm.readWriteVars;
            this.allVars = cm.allVars;
            this.parentVars = cm.parentVars;
            this.allowableValues = cm.allowableValues;
            this.mandatory = cm.mandatory;
            this.allMandatory = cm.allMandatory;
            this.imports = cm.imports;
            this.hasVars = cm.hasVars;
            this.emptyVars = cm.emptyVars;
            this.hasMoreModels = cm.hasMoreModels;
            this.hasEnums = cm.hasEnums;
            this.isEnum = cm.isEnum;
            this.hasRequired = cm.hasRequired;
            this.hasOptional = cm.hasOptional;
            this.isArray = cm.isArray;
            this.hasChildren = cm.hasChildren;
            this.hasOnlyReadOnly = cm.hasOnlyReadOnly;
            this.externalDocumentation = cm.externalDocumentation;
            this.vendorExtensions = cm.vendorExtensions;
            this.additionalPropertiesType = cm.additionalPropertiesType;

            this.hasImports = !this.imports.isEmpty();
        }

        public boolean hasComplexVars() {
            for (CodegenProperty p : vars) {
                if (!p.isPrimitiveType) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String escapeQuotationMark(String input) {
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        // no need to escape as Elixir does not support multi-line comments
        return input;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public GeneratorLanguage generatorLanguage() { return GeneratorLanguage.ELIXIR; }

    public enum CasingType {CAMEL_CASE, SNAKE_CASE};

    /**
     * General purpose sanitizing function for Elixir identifiers (fields, variables, structs, parameters, etc.).<br>
     * @param name The input string
     * @param casingType Which casing type to apply
     * @param escapePrefix Prefix to escape words beginning with numbers or reserved words
     * @param type The type of identifier (used for logging)
     * @param allowRawIdentifiers Raw identifiers can't always be used, because of filename vs import mismatch.
     * @return Sanitized string
     */
    public String sanitizeIdentifier(String name, ElixirClientCodegen.CasingType casingType, String escapePrefix, String type, boolean allowRawIdentifiers) {
        String originalName = name;

        Function<String, String> casingFunction;
        switch (casingType) {
            case CAMEL_CASE:
                // This probably seems odd, but it is necessary for two reasons
                // Compatibility with rust-server, such that MyIDList => my_id_list => MyIdList
                // Conversion from SCREAMING_SNAKE_CASE to ScreamingSnakeCase
                casingFunction = (input) -> camelize(underscore(input));
                break;
            case SNAKE_CASE:
                casingFunction = org.openapitools.codegen.utils.StringUtils::underscore;
                break;
            default:
                throw new IllegalArgumentException("Unknown CasingType");
        }

        // Replace hyphens with underscores
        name = name.replaceAll("-", "_");

        // Apply special character escapes, e.g. "@type" => "At_type"
        // Remove the trailing underscore if necessary
        if (!Strings.isNullOrEmpty(name)) {
            boolean endedWithUnderscore = name.endsWith("_");
            name = escape(name, specialCharReplacements, charactersToAllow, "_");
            if (!endedWithUnderscore && name.endsWith("_")) {
                name = org.apache.commons.lang3.StringUtils.chop(name);
            }
        }

        // Sanitize any other special characters that weren't replaced
        name = sanitizeName(name);

        // Keep track of modifications prior to casing
        boolean nameWasModified = !originalName.equals(name);

        // Convert casing
        name = casingFunction.apply(name);

        // If word starts with number add a prefix
        // Note: this must be done after casing since CamelCase will strip leading underscores
        if (name.matches("^\\d.*")) {
            nameWasModified = true;
            name = casingFunction.apply(escapePrefix + '_' + name);
        }

        // Escape reserved words - this is case-sensitive so must be done after casing
        // There is currently a bug in Rust where this doesn't work for a few reserved words :(
        // https://internals.rust-lang.org/t/raw-identifiers-dont-work-for-all-identifiers/9094
        if (isReservedWord(name)) {
            nameWasModified = true;
            if (this.keywordsThatDoNotSupportRawIdentifiers.contains(name) || !allowRawIdentifiers) {
                name = casingFunction.apply(escapePrefix + '_' + name);
            } else {
                name = "r#" + name;
            };
        }

        // If the name had to be modified (not just because of casing), log the change
        if (nameWasModified) {
            LOGGER.warn("{} cannot be used as a {} name. Renamed to {}", casingFunction.apply(originalName), type, name);
        }

        return name;
    }

    @Override
    public String toVarName(String name) {
        return sanitizeIdentifier(name, CasingType.SNAKE_CASE, "param", "field/variable", true);
    }

    @Override
    public String toParamName(String name) {
        return sanitizeIdentifier(name, CasingType.SNAKE_CASE, "param", "parameter", true);
    }
}
