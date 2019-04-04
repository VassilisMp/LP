package gr.teilar;

import org.mariuszgromada.math.mxparser.Argument;

import java.util.Objects;

public class Variable {
    Argument variable;
    int factor;

    public Variable(String variable, int factor) {
        this.variable = new Argument(variable, 0);
        this.factor = factor;
    }

    public Variable(String variable) {
        this(variable, 0);
    }

    @Override
    public String toString() {
        return "Variable{" + factor + "" + variable.getArgumentName() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable)) return false;

        Variable variable1 = (Variable) o;

        if (factor != variable1.factor) return false;
        return Objects.equals(variable, variable1.variable);
    }

    @Override
    public int hashCode() {
        int result = variable != null ? variable.hashCode() : 0;
        result = 31 * result + factor;
        return result;
    }
}
