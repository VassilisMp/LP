package gr.teilar;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tableau {
    Simplex simplex;
    double[][] tableau;
    List<Variable> col;
    Variable enteringVar;
    Variable leavingVar;
    int[] pivot;
    boolean stop;

    public Tableau(Simplex simplex, List<Variable> bVars, List<Variable[]> trConstraintsVars, List<String> trConstraints) throws Exception {
        this.simplex = simplex;
        this.col = bVars;
        this.tableau = new double[simplex.iLength][simplex.jLength];

        // put values in the initial tableau from the constraints equations
        for (int i = 0; i < simplex.iLength-2; i++) {
            for (int j = 0; j < simplex.jLength-1; j++) {
                int finalJ = j;
                int finalI = i;
                Arrays.stream(trConstraintsVars.get(i))
                        .filter(v -> v.variable.getArgumentName().equals(simplex.allVars.get(finalJ).variable.getArgumentName()))
                        .findAny()
                        .ifPresent(v -> tableau[finalI][finalJ] = v.factor);
            }
        }
        // Cj-Zj calculate
        for (int i = 0; i < tableau[0].length - 2; i++) {
            tableau[tableau.length - 1][i] = simplex.allVars.get(i).factor;
        }
        // check if optimal solution has been found
        stop = true;
        if (simplex.max_min) {
            for (int i = 0; i < simplex.jLength; i++) {
                if (tableau[simplex.iLength-1][i] > 0) {
                    stop = false;
                    break;
                }
            }
        }
        else {
            for (int i = 0; i < simplex.jLength; i++) {
                if (tableau[simplex.iLength-1][i] < 0) {
                    stop = false;
                    break;
                }
            }
        }
        if (!stop) {
            Variable variable1;
            if (simplex.max_min) {
                variable1 = simplex.allVars.stream()
                        .max(Comparator.comparingInt(variable -> variable.factor))
                        .orElseThrow(NoSuchElementException::new);
            } else {
                variable1 = simplex.allVars.stream()
                        .min(Comparator.comparingInt(variable -> variable.factor))
                        .orElseThrow(NoSuchElementException::new);
            }
            int index = simplex.allVars.indexOf(variable1);
            // entering Variable in next tableau
            enteringVar = simplex.allVars.get(index);

            // bi from equalities
            int minIndex = -1;
            tableau[0][tableau[0].length - 1] = Double.parseDouble(trConstraints.get(0).split("=")[1]);
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < this.col.size(); i++) {
                tableau[i][tableau[i].length - 1] = Double.parseDouble(trConstraints.get(i).split("=")[1]);
                if (tableau[i][index] != 0 && tableau[i][tableau[i].length - 1] / tableau[i][index] < min) {
                    min = tableau[i][tableau[i].length - 1] / tableau[i][index];
                    minIndex = i;
                }
            }
            if (minIndex == -1) throw new Exception("Couldn't find variable leaving in the first tableau");
            leavingVar = this.col.get(minIndex);
            pivot = new int[]{minIndex, index};
        }
    }

    private Tableau(){}

    public Tableau getNextTableau() throws Exception {
        Tableau tableau = new Tableau();
        tableau.simplex = this.simplex;
        tableau.col = new ArrayList<>(this.col);
        // insert the new variable in the row replacing the leaving
        tableau.col.set(this.pivot[0], simplex.allVars.get(this.pivot[1]));
        // make a copy of the previous tableau to the new one
        tableau.tableau = new double[this.tableau.length][];
        IntStream.range(0, this.tableau.length).forEach(i -> tableau.tableau[i] = Arrays.copyOf(this.tableau[i], this.tableau[i].length));
        // division of the pivot line by the pivot
        for (int i = 0; i < simplex.jLength; i++) {
            tableau.tableau[this.pivot[0]][i] /= this.tableau[this.pivot[0]][this.pivot[1]];
        }
        // calculate values of the new tableau
        for (int i = 0; i < simplex.jLength; i++) {
            Variable var = null;
            if (i < simplex.allVars.size())
                var = simplex.allVars.get(i);
            Variable finalVar = var;
            boolean anyMatch = tableau.col.stream()
                    .anyMatch(variable -> Objects.equals(variable, finalVar));
            if (anyMatch) {
                // calculate columns of basic variables intersection
                for (int j = 0; j < simplex.iLength - 2; j++) {
                    if (j == this.pivot[0])
                        continue;
                    if (tableau.col.get(j).equals(simplex.allVars.get(i))) {
                        tableau.tableau[j][i] = 1;
                        continue;
                    }
                    tableau.tableau[j][i] = 0;
                }
            } else {
                // calculate columns where there is no basic variables intersection
                for (int j = 0; j < simplex.iLength - 2; j++) {
                    if (j == this.pivot[0])
                        continue;
                    tableau.tableau[j][i] = this.tableau[j][i] - ((this.tableau[this.pivot[0]][i]*this.tableau[j][this.pivot[1]])/this.tableau[this.pivot[0]][this.pivot[1]]);
                }
            }
        }
        // Zj and Cj-Zj calculate
        for (int i = 0; i < simplex.jLength-1; i++) {
            double sum = 0.0;
            for (int j = 0; j < simplex.iLength - 2; j++) {
                sum += tableau.tableau[j][i] * tableau.col.get(j).factor;
            }
            tableau.tableau[simplex.iLength-1][i] = simplex.allVars.get(i).factor - (tableau.tableau[simplex.iLength-2][i] = sum);
        }
        // last column of Zj calculate
        {
            double sum = IntStream.range(0, simplex.iLength - 2).mapToDouble(i -> tableau.tableau[i][simplex.jLength - 1] * tableau.col.get(i).factor).sum();
            tableau.tableau[simplex.iLength - 2][simplex.jLength - 1] = sum;
        }
        // check if optimal solution has been found
        tableau.stop = true;
        if (simplex.max_min) {
            if (IntStream.range(0, simplex.jLength).anyMatch(i -> tableau.tableau[simplex.iLength - 1][i] > 0)) tableau.stop = false;
        }
        else {
            if (IntStream.range(0, simplex.jLength).anyMatch(i -> tableau.tableau[simplex.iLength - 1][i] < 0)) tableau.stop = false;
        }
        if (!tableau.stop) {
            int index = -1;
            double maxORmin;
            if (simplex.max_min) {
                maxORmin = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < simplex.jLength - 1; i++) {
                    if (tableau.tableau[simplex.iLength - 1][i] > maxORmin) {
                        maxORmin = tableau.tableau[simplex.iLength - 1][i];
                        index = i;
                    }
                }
            }
            else {
                maxORmin = Double.POSITIVE_INFINITY;
                for (int i = 0; i < simplex.jLength - 1; i++) {
                    if (tableau.tableau[simplex.iLength - 1][i] < maxORmin) {
                        maxORmin = tableau.tableau[simplex.iLength - 1][i];
                        index = i;
                    }
                }
            }
            if (index == -1) throw new Exception("Couldn't find variable entering in the " + simplex.tableaus.size()+1 + " tableau");
            // entering Variable in next tableau
            tableau.enteringVar = simplex.allVars.get(index);
            // leaving variable in next tableau
            int minIndex = -1;
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < col.size(); i++)
                if (tableau.tableau[i][index] > 0 && tableau.tableau[i][simplex.jLength - 1] / tableau.tableau[i][index] < min) {
                    min = tableau.tableau[i][simplex.jLength - 1] / tableau.tableau[i][index];
                    minIndex = i;
                }
            if (minIndex == -1) throw new Exception("Couldn't find variable leaving in the " + simplex.tableaus.size()+1 + " tableau");
            tableau.leavingVar = tableau.col.get(minIndex);
            tableau.pivot = new int[]{minIndex, index};
        }
        return tableau;
    }

    @Override
    public String toString() {
        return "Tableau{" + "\n" +
                "tableau=" + "\n" +  Arrays.stream(tableau).map(arr -> Arrays.toString(arr) + "\n").collect(Collectors.joining()) +
                ", col=" + col + "\n" +
                ", enteringVar=" + enteringVar + "\n" +
                ", leavingVar=" + leavingVar + "\n" +
                ", pivot=" + Arrays.toString(pivot) + "\n" +
                ", stop=" + stop + "\n" +
                '}';
    }
}
