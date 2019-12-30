package projects.Assembler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import projects.Assembler.Parser.CommandType;
import java.util.Map;

// Initializes I/O and drives the process
class HackAssembler {
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java HackAssembler <input file name>");
            return;
        }

        final String inFilename = args[0];
        final String[] parts = inFilename.split(Pattern.quote("."));
        final String outFilename = parts[0] + ".hack";

        Parser parser = new Parser(inFilename);
        final Code code = new Code();
        final SymbolTable symbols = new SymbolTable();

        // First pass - just the jump labels.
        int currentInstruction = 0;
        while (parser.nextToken()) {
            if (parser.currentCommandType() == CommandType.Label) {
                String var = parser.label();
                symbols.insert(var, String.format("%d", currentInstruction + 1));
            } else {
                currentInstruction ++; // Labels will be removed from output, so don't count them.
            }
        }

        // Second pass - just non-jump label variables.
        int currentVarAddress = 16;
        parser.reset();
        while (parser.nextToken()) {
            if (parser.currentCommandType() == CommandType.ACommand) {
                // Map to consecutive memory locations starting at 16 from first definition position.
                String var = parser.label();
                if (!isInt(var) && !symbols.exists(var)) {
                    // Can't parse as an int, so we know it's a variable.
                    // If it was a label, it's already in the symbol table, so only add new variables.
                    symbols.insert(var, String.format("%d", currentVarAddress ++));
                }
            }
        }

        // Third pass - output binary now that our symbol table is built.
        PrintWriter pw;
        try {
            pw = new PrintWriter(outFilename);
        } catch (final FileNotFoundException e) {
            e.printStackTrace(); // Should not be hit since we just created the file.
            return;
        }

        parser.reset();
        while (parser.nextToken()) {
            switch (parser.currentCommandType()) {
            case ACommand:
                String address = parser.label();
                if (!isInt(address)) {
                    address = symbols.addressForSymbol(address); // Symbol table lookup.
                }

                pw.println("0" + code.address(address));
                break;
            case CCommand:
                final String c = parser.comp();
                final String d = parser.dest();
                final String j = parser.jump();
                pw.println("111" + code.comp(c) + code.dest(d) + code.jump(j));
                break;
            case Label:

                break;
            }
        }

        pw.close();
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

// Unpack each instruction into underlying fields.
class Parser {
    List<String> lines;
    int currentIndex;

    public Parser(final String filename) {
        lines = readFileIntoList(filename);
        currentIndex = -1;
    }

    private static List<String> readFileIntoList(final String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);

            List<String> newLines = new ArrayList<String>();
            for (String s : lines) {
                s = s.replaceAll("//.*$", ""); // Replace comments with nothing.
                s = s.trim();
                if (s.length() > 0) {
                    newLines.add(s);
                }
            }

            lines = newLines;
        }

        catch (final IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public void reset() {
        currentIndex = -1;
    }

    public Boolean nextToken() {
        currentIndex++;
        return currentIndex < lines.size();
    }

    public CommandType currentCommandType() {
        final String line = lines.get(currentIndex);
        if (line.startsWith("(")) {
            return CommandType.Label;
        } else if (line.startsWith("@")) {
            return CommandType.ACommand;
        } else {
            return CommandType.CCommand;
        }
    }

    public String dest() {
        final CommandType commandType = currentCommandType();
        final String line = lines.get(currentIndex);

        if (commandType == CommandType.CCommand) {
            final String[] parts = line.split("=");
            if (parts.length == 2) {
                return parts[0].trim();
            } else {
                return null;
            }
        } else {
            return null; // Doesn't have a dest.
        }
    }

    public String comp() {
        final CommandType commandType = currentCommandType();
        final String line = lines.get(currentIndex);

        if (commandType == CommandType.CCommand) {
            int equalIndex = line.indexOf("=");
            int semicolonIndex = line.indexOf(";");

            if (equalIndex != -1 && semicolonIndex == -1) {
                semicolonIndex = line.length();
            } else if (semicolonIndex != -1 && equalIndex == -1) {
                equalIndex = -1; // Take from front.
            }

            return line.substring(equalIndex + 1, semicolonIndex).trim();
        } else {
            return null; // Doesn't have a dest.
        }
    }

    public String jump() {
        final CommandType commandType = currentCommandType();
        final String line = lines.get(currentIndex);

        if (commandType == CommandType.CCommand) {
            final String[] parts = line.split(";");
            if (parts.length == 2) {
                return parts[1].trim();
            } else {
                return null;
            }
        } else {
            return null; // Doesn't have a jump.
        }
    }

    public String label() {
        final CommandType commandType = currentCommandType();
        final String line = lines.get(currentIndex);

        if (commandType == CommandType.ACommand) {
            return line.substring(1);
        } else if (commandType == CommandType.Label) {
            return line.substring(1, line.length() - 1);
        } else {
            return null; // Doesn't have a label.
        }
    }

    public enum CommandType {
        ACommand, CCommand, Label
    }
}

// Translates each field into corresponding binary value.
class Code {
    public String dest(String mnemonic) {
        if (mnemonic == null) {
            mnemonic = "";
        }

        switch (mnemonic) {
        case "":
        default:
            return "000";
        case "M":
            return "001";
        case "D":
            return "010";
        case "MD":
            return "011";
        case "A":
            return "100";
        case "AM":
            return "101";
        case "AD":
            return "110";
        case "AMD":
            return "111";
        }
    }

    public String comp(String mnemonic) {
        if (mnemonic == null) {
            mnemonic = "";
        }

        switch (mnemonic) {
        case "":
        default:
            return "?";
        case "0":
            return "0101010";
        case "1":
            return "0111111";
        case "-1":
            return "0111010";
        case "D":
            return "0001100";
        case "A":
        case "M": {
            String prefix = mnemonic.equals("A") ? "0" : "1";
            return prefix + "110000";
        }
        case "!D":
            return "0001101";
        case "!A":
        case "!M": {
            String prefix = mnemonic.equals("!A") ? "0" : "1";
            return prefix + "110001";
        }
        case "-D":
            return "0001111";
        case "-A":
        case "-M": {
            String prefix = mnemonic.equals("-A") ? "0" : "1";
            return prefix + "110011";
        }
        case "D+1":
            return "0011111";
        case "A+1":
        case "M+1": {
            String prefix = mnemonic.equals("A+1") ? "0" : "1";
            return prefix + "110111";
        }
        case "D-1":
            return "0001110";
        case "A-1":
        case "M-1": {
            String prefix = mnemonic.equals("A-1") ? "0" : "1";
            return prefix + "110010";
        }
        case "D+A":
        case "D+M": {
            String prefix = mnemonic.equals("D+A") ? "0" : "1";
            return prefix + "000010";
        }
        case "D-A":
        case "D-M": {
            String prefix = mnemonic.equals("D-A") ? "0" : "1";
            return prefix + "010011";
        }
        case "A-D":
        case "M-D": {
            String prefix = mnemonic.equals("A-D") ? "0" : "1";
            return prefix + "000111";
        }
        case "D&A":
        case "D&M": {
            String prefix = mnemonic.equals("D&A") ? "0" : "1";
            return prefix + "000000";
        }
        case "D|A":
        case "D|M": {
            String prefix = mnemonic.equals("D|A") ? "0" : "1";
            return prefix + "010101";
        }
        }
    }

    public String jump(String mnemonic) {
        if (mnemonic == null) {
            mnemonic = "";
        }

        switch (mnemonic) {
        case "":
        default:
            return "000";
        case "JGT":
            return "001";
        case "JEQ":
            return "010";
        case "JGE":
            return "011";
        case "JLT":
            return "100";
        case "JNE":
            return "101";
        case "JLE":
            return "110";
        case "JMP":
            return "111";
        }
    }

    public String address(final String mnemonic) {
        final int address = Integer.parseInt(mnemonic);
        String s = Integer.toBinaryString(address);
        while (s.length() < 15) {
            s = '0' + s;
        }

        return s;
    }
}

// Manages symbol table.
class SymbolTable {
    private Map<String, String> map;

    public SymbolTable() {
        map = new HashMap<>();

        // Add predefined symbols.
        insert("SP", "0");
        insert("LCL", "1");
        insert("ARG", "2");
        insert("THIS", "3");
        insert("THAT", "4");
        insert("SCREEN", "16384");
        insert("KBD", "24576");
        
        for (int i = 0; i < 16; ++i) {
            insert("R" + i, String.format("%d", i));
        }
    }

    public void insert(String symbol, String address) {
        map.put(symbol, address);
    }

    public boolean exists(String symbol) {
        return map.get(symbol) != null;
    }

    public String addressForSymbol(String symbol) {
        return map.get(symbol);
    }
}