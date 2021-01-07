package fi.metatavu.airquality.configuration_parsing;

public class RequestParameters {
    private String name;
    private String variable;
    private ParameterType parameterType;
    private String formula;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }
}
enum ParameterType {
    TRESHOLD,
    TIME
}
