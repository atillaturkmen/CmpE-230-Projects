#!/usr/bin/env python3

import sys
from MyCPU import MyCPU

# Read name of the input file
input_name = sys.argv[1]
# Name of the output file is same with different extension
output_name = input_name[:-4] + ".txt"

# Read all instructions
instructions = []
with open(input_name, "r") as input_file:
    for line in input_file:
        line = line.strip()
        if len(line) == 0:
            continue
        binary_instruction = format(int(line, 16), '024b')
        instructions.append(binary_instruction)

# Create a MyCPU instance and execute the instructions
cpu = MyCPU()
output = cpu.compute(instructions)

# Write outputs to file
with open(output_name, "w") as output_file:
    for char in output:
        output_file.write(char + "\n")
