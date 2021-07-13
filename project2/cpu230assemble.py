#!/usr/bin/env python3

import re
import sys


def is_label(x: str):
    """
    Returns true if given string is a label

    :param x: Line that will be checked if it is a label
    :return: None
    """
    return len(x.split()) == 1 and x.strip()[-1:] == ':'


hex_regex = re.compile(r"0*[0-9a-fA-F]{1,4}")
character_regex = re.compile(r"['\"`‘].['\"`’]")

# Hex codes of the instructions
instructions = {
    "HALT": "1",
    "LOAD": "2",
    "STORE": "3",
    "ADD": "4",
    "SUB": "5",
    "INC": "6",
    "DEC": "7",
    "XOR": "8",
    "AND": "9",
    "OR": "A",
    "NOT": "B",
    "SHL": "C",
    "SHR": "D",
    "NOP": "E",
    "PUSH": "F",
    "POP": "10",
    "CMP": "11",
    "JMP": "12",
    "JZ": "13",
    "JE": "13",
    "JNZ": "14",
    "JNE": "14",
    "JC": "15",
    "JNC": "16",
    "JA": "17",
    "JAE": "18",
    "JB": "19",
    "JBE": "1A",
    "READ": "1B",
    "PRINT": "1C",
}

# Addressing modes to correct opcodes
instruction_modes = {
    "00": {"2", "4", "5", "6", "7", "8", "9", "A", "B", "11", "12",
           "13", "14", "15", "16", "17", "18", "19", "1A", "1C"},
    "01": {"2", "3", "4", "5", "6", "7", "8", "9", "A",
           "B", "C", "D", "F", "10", "11", "1B", "1C"},
    "10": {"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "11", "1B", "1C"},
    "11": {"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "11", "1B", "1C"},
    "no operand": {"1", "E"},
}

# Hex codes of registers
registers = {
    "A": "0001",
    "B": "0002",
    "C": "0003",
    "D": "0004",
    "E": "0005",
    "S": "0006",
}

# Read name of the input file
input_name = sys.argv[1]
# Name of the output file is same with different extension
output_name = input_name[:-4] + ".bin"

# Stores memory addresses of labels
labels = dict()

# Fill labels dictionary
# First pass
with open(input_name, "r") as input_file:
    label_address = 0
    for line in input_file:
        line = line.strip()
        # Add it to labels if there is one token and last character is ':'
        if len(line) == 0:
            continue
        if is_label(line):
            labels[line[:-1]] = format(label_address, 'X')
        # Increment memory addresses by 3 because one instruction is 3 bytes
        else:
            label_address += 3

# Open input, output files and assemble the code
# Second pass
with open(input_name, 'r') as reader, open(output_name, 'w') as writer:
    for line in reader:
        line = line.strip()
        # Ignore labels and empty lines
        if is_label(line) or len(line) == 0:
            continue
        # Tokenize the line
        line = line.split()
        # Get opcode and add it to binary instruction of this line
        opcode = instructions[line[0]]
        binary_instruction = format(int(opcode, 16), '06b')

        # Raise exception if there is more than two tokens on this line
        if len(line) > 2:
            raise Exception("More than two tokens on one line")

        # Default values if there is no operand
        addressing_mode = "no operand"
        binary_operand = "0" * 16

        # Read operand if there is one
        if len(line) == 2:
            operand = line[1]

            # Operand is memory address
            if operand[0] == "[" and operand[-1] == "]":
                operand = operand[1:-1]
                # Address is in the register
                if operand in registers:
                    addressing_mode = "10"
                    operand = registers[operand]
                # Address is given in hex
                elif hex_regex.match(operand):
                    addressing_mode = "11"
                else:
                    raise Exception("operand is undefined inside [] in line: ", line)

            # Operand is register
            elif operand in registers:
                addressing_mode = "01"
                operand = registers[operand]

            # Operand is label
            elif operand in labels:
                addressing_mode = "00"
                operand = labels[operand]

            # Operand is letter, get hex ASCII value
            elif character_regex.match(operand):
                addressing_mode = "00"
                operand = format(ord(operand[1]), "X")

            # Operand is a hex number
            elif hex_regex.match(operand):
                addressing_mode = "00"

            # Raise exception if operand is none of the above
            else:
                raise Exception("operand is undefined in line: ", line)

            # Convert hex operand to binary
            binary_operand = format(int(operand, 16), '016b')

        # Raise exception if operand and opcode is incompatible
        if opcode not in instruction_modes[addressing_mode]:
            raise Exception("Wrong addressing mode in line: ", line)

        # Addressing mode is zero if there is no operand
        if addressing_mode == "no operand":
            addressing_mode = "00"

        # Concatenate the binary instruction
        binary_instruction += addressing_mode + binary_operand
        # Convert it to hex
        hex_instruction = format(int(binary_instruction, 2), '06X')
        # Write the instruction to the file
        writer.write(hex_instruction + "\n")
