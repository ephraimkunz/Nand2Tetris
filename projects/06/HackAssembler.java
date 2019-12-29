
// Initializes I/O and drives the process
class HackAssembler {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: javac HackAssembler <input file name>");
            return;
        }

        String filename = args[0];
    }
}

// Unpack each instruction into underlying fields.
class Parser {
}

// Translates each field into corresponding binary value.
class Code {

}

// Manages symbol table.
class SymbolTable {

}