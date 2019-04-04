package gr.teilar;

import org.jetbrains.annotations.NotNull;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.parsertokens.Token;

import java.util.*;
import java.util.stream.Collectors;

public class Simplex {
    // Objective Function
    private String objFun;
    // Restrictions List
    private List<String> constraints;
    // maximize or minimize objective function
    // max is true and min is false
    boolean max_min;
    List<Variable> objFunVars;
    // transformed Restrictions List, added slack variables and replaced inequalities with equalities
    private List<String> trConstraints;
    // transformed Constraints variables
    private List<Variable[]> trConstraintsVars;
    // list of Tableaus for this problem
    List<Tableau> tableaus;
    // list of Slack variables
    private List<Variable> SlackVars;
    // list of Surplus variables
    private List<Variable> SurplusVars;
    // list of artificial variables;
    private List<Variable> artVars;
    // all variables of the problem and also the first tableau row
    List<Variable> allVars;
    int iLength;
    int jLength;

    public Simplex(String objFun, boolean max_min, List<String> constraints) throws Exception {
        this.objFun = objFun;
        this.max_min = max_min;
        this.constraints = constraints;
        this.SlackVars = new ArrayList<>();
        this.SurplusVars = new ArrayList<>();
        this.artVars = new ArrayList<>();
        this.allVars = new ArrayList<>();
        List<Variable> fCol = new ArrayList<>();
        this.allVars.addAll(getFunctionVariables(objFun));
        this.objFunVars= new ArrayList<>(this.allVars);
        // transformed objective function variables
        try {
            this.trConstraints = transformConstraints(this.constraints, fCol);
            List<Variable> tempList = new ArrayList<>();
            this.artVars.forEach(v -> tempList.add(new Variable(v.variable.getArgumentName())));
            this.allVars.addAll(tempList);
            //this.trConstraints.forEach(c -> );
            this.trConstraintsVars = new ArrayList<>(trConstraints.size());
            this.trConstraints.forEach(s -> trConstraintsVars.add(getFunctionVariables(s).toArray(new Variable[0])));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.tableaus = new ArrayList<>();
        iLength = fCol.size()+2;
        jLength = allVars.size()+1;
        Tableau tableau = new Tableau(this, fCol, Objects.requireNonNull(trConstraintsVars, "trConstraintsVars must be not null"), trConstraints);
        this.tableaus.add(tableau);
        while (!tableau.stop) {
            tableau = tableau.getNextTableau();
            this.tableaus.add(tableau);
        }
        this.objFunVars.forEach(variable -> {
            Tableau table = this.tableaus.get(tableaus.size() - 1);
            int indexOf = table.col.indexOf(variable);
            if (indexOf != -1)
                variable.variable.setArgumentValue(table.tableau[indexOf][jLength-1]);
        });
    }

    @Override
    public String toString() {
        return "Simplex{" + "\n" +
                "objFun='" + objFun + '\'' +
                ", constraints=" + "\n" + constraints +
                ", max_min=" + max_min +
                ", trConstraints=" + "\n" + trConstraints +
                ", tableaus=" + "\n" + tableaus +
                ", allVars=" + "\n" + allVars +
                ", iLength=" + "\n" + iLength +
                ", jLength=" + "\n" + jLength +
                ", solution: " + objFunVars.stream().map(arr -> arr.variable.getArgumentName() + "=" + arr.variable.getArgumentValue() + ", ").collect(Collectors.joining()) +
                "function value: " + this.tableaus.get(tableaus.size() - 1).tableau[iLength-2][jLength-1] +
                '}';
    }

    private List<String> transformConstraints(@NotNull List<String> constraints, List<Variable> fCol) {
        List<String> list = new ArrayList<>();
        int sCounter = 0;
        int aCounter = 0;
        for (String s : constraints) {
            StringBuilder finalSb = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            if (s.contains(">=")) {
                finalSb.append(s.replaceFirst(">", ""));
                sb.append("-s").append(++sCounter).append("+A").append(++aCounter);
                Variable surVar = new Variable("s" + sCounter, -1);
                this.SurplusVars.add(surVar);
                Variable artVar = new Variable("A" + aCounter, 1);
                this.artVars.add(artVar);
                fCol.add(new Variable(artVar.variable.getArgumentName(), 0));
                this.allVars.add(new Variable(surVar.variable.getArgumentName(), 0));
            } else if (s.contains("<=")) {
                finalSb.append(s.replaceFirst("<", ""));
                sb.append("+s").append(++sCounter);
                Variable slackVar = new Variable("s" + sCounter, 1);
                this.SlackVars.add(slackVar);
                slackVar = new Variable(slackVar.variable.getArgumentName(), 0);
                fCol.add(slackVar);
                this.allVars.add(slackVar);
            } else {
                finalSb.append(s);
                sb.append("+A").append(++aCounter);
                Variable artVar = new Variable("A" + aCounter, 1);
                this.artVars.add(artVar);
                fCol.add(new Variable(artVar.variable.getArgumentName(), 0));
            }
            int indexOf = finalSb.indexOf("=");
            finalSb.insert(indexOf, sb);
            list.add(finalSb.toString());
        }
        return list;
    }

    private static List<Variable> getFunctionVariables(String str) {
        Expression expression = new Expression(str);
        String[] missingArguments = expression.getMissingUserDefinedArguments();
        List<Variable> variables = new ArrayList<>();
        //Arrays.stream(missingArguments).forEach(s -> variables.add(new Variable(s)));
        List<Token> tokens = expression.getCopyOfInitialTokens();
        for (String arg :
                missingArguments) {
            for (int i = 0; i < tokens.size(); i++) {
                try {
                    if (tokens.get(i).tokenStr.equals(arg) && i==0) {
                        variables.add(
                                new Variable(arg, 1)
                        );
                        //System.out.println("1 " + arg);
                    } else if (tokens.get(i).tokenStr.equals(arg) && tokens.get(i - 1).tokenStr.equals("-")){
                        variables.add(
                                new Variable(arg, -1)
                        );
                        //System.out.println("-1 " + arg);
                    } else if (tokens.get(i).tokenStr.equals(arg) && !tokens.get(i - 1).tokenStr.equals("*")) {
                        variables.add(
                                new Variable(arg, 1)
                        );
                        //System.out.println("1 " + arg);
                    } else if (tokens.get(i).tokenStr.equals(arg) && tokens.get(i - 1).tokenStr.equals("*") && tokens.get(i - 2).keyWord.equals("_num_")) {
                        String tokenStr = tokens.get(i - 2).tokenStr;
                        if (i>2 && tokens.get(i - 3).tokenStr.equals("-"))
                            tokenStr = '-' + tokenStr;
                        variables.add(
                                new Variable(arg, Integer.parseInt(tokenStr))
                        );
                        //System.out.println(tokenStr + " " + arg);
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }
        //variables.forEach(System.out::println);
        return variables;
    }
}
