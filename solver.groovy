import groovy.transform.Field
import groovyjarjarantlr.collections.List
import org.testng.collections.ListMultiMap

@Field def assignment = []
@Field int[] occ
@Field int numberOfVars
@Field def numberOfClauses
@Field ListMultiMap<Integer, List> map = ListMultiMap.create()
@Field def clauses = []
@Field def vars = []
@Field boolean flag = false
@Field def max_flips = Integer.parseInt(args[1])
@Field def max_tries = Integer.parseInt(args[2])
@Field def noise = Float.parseFloat(args[3])
@Field def unsat_clause = []
@Field def currentUnsatClauses = []
@Field Random random = new Random();

def getData() {
    def cnfFile = new File(args[0])
    def lines = cnfFile.readLines()
    for (String l : lines) {
        l = l.trim()
        if (!l.startsWith("c")) {
            if (l.startsWith("p")) {
                def s = l.split(" ")
                numberOfVars = Integer.parseInt(s[2])
                numberOfClauses = Integer.parseInt(s[3])
                assignment = new Integer[numberOfVars + 1]
                assignment[0] = 0
            } else {
                def s = l.split(" ")
                def singleClause = []
                def itr = s.iterator()
                singleClause = itr.collectMany { Integer.parseInt(it) == 0 ? [] : [Integer.parseInt(it)] }
                singleClause = singleClause.unique()
                singleClause = singleClause.sort()
                for (int lit : singleClause) {
                    map.put(lit, singleClause)
                }
                clauses << singleClause
            }
        }
    }
}

getData()
clauses.unique()
businessLogic()

def randomlyGenerateTruthAssignment() {
    (1..numberOfVars).each {
        assignment[it] = Math.random() > 0.5 ? 1 : -1
    }
}

//Algorithm implementation...
def businessLogic() {
    for (def tries = 0; tries < max_tries; ++tries) {
        randomlyGenerateTruthAssignment()
        for (def flip = 0; flip < max_flips; ++flip) {
            if (checkIfFormulaSAT()) {
                println("s SATISFIABLE")
                displayResult()
                return true
            } else { //if some clause is UNSAT...
                def random_unsat = findUnsatClauseRandomlyForCurrentAssignment()
                def info = litWithLeastBreakCount(random_unsat)
                if (info[0] == 0) {
                    assignment[Math.abs(info[1])] *= -1
                } else {
                    if (Math.random() <= noise)
                        assignment[Math.abs(random_unsat[random.nextInt(random_unsat.size())])] *= -1
                    else assignment[Math.abs(info[1])] *= -1
                }
            }
        }
    }
    println("s UNSATISFIABLE")
}

def findUnsatClauseRandomlyForCurrentAssignment() {
    currentUnsatClauses = []
    clauses.each {
        clause ->
            if(!checkIfClauseSAT(clause)) currentUnsatClauses << clause
    }
    return currentUnsatClauses[random.nextInt(currentUnsatClauses.size())]
}

def checkIfFormulaSAT() {
    for (ArrayList clause : clauses) {
        if (clause.every {
            assignment[Math.abs(it)] * it < 0
        }) {
            return 0
        }
    }
    return 1
}

def checkIfClauseSAT(clause) {
    if (clause.every {
        assignment[Math.abs(it)] * it < 0
    }) {
        return 0
    }
    return 1
}

def litWithLeastBreakCount(clause) {
    println(clause)
    def min = numberOfClauses
    def candidate_lit = 0
    clause.each {
        lit ->
            def count = 0
            assignment[Math.abs(lit)] *= -1
            def ref_clauses = map.get(lit)
            ref_clauses.each {
                single_clause ->
                    if (!checkIfClauseSAT(single_clause)) count++
            }
            assignment[Math.abs(lit)] *= -1
            if (count < min) {
                min = count
                candidate_lit = lit
            }
    }
    return [min, candidate_lit]
}

def displayResult() {
    flag = true
    print("v ")
    for (int i = 1; i <= numberOfVars; i++) {
        print(assignment[i] * i + " ")
    }
}
