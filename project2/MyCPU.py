class MyCPU:

    def __init__(self):

        # CPU values
        self.ZF = self.CF = self.SF = False
        self.PC = 0
        self.A = self.B = self.C = self.D = self.E = "0" * 16
        self.S = []

        # Memory
        self.memory = dict()

        # Contains printed chars in execution
        self.output = []

        # True when program reaches a HALT instruction
        self.halt = False

    def get_value_in_register(self, register_no):
        """
        Returns value in register

        :param register_no: Hex value of register
        :return: Value in that register
        """
        if register_no == "0001":
            return self.A
        if register_no == "0002":
            return self.B
        if register_no == "0003":
            return self.C
        if register_no == "0004":
            return self.D
        if register_no == "0005":
            return self.E

    def get_value(self, mode, operand):
        """
        Retrieves the value that will be used according to address mode

        :param mode: 2 bit addressing mode
        :param operand: 16 bit operand
        :return: operand that will be used in instruction
        """
        # Immediate use of value
        if mode == "00":
            return operand
        else:
            operand = format(int(operand, 2), "04X")
            # Value is in register
            if mode == "01":
                return self.get_value_in_register(operand)
            # Value is in address in the register
            if mode == "10":
                operand = self.get_value_in_register(operand)
            # Return the value that is in that memory address, zero if address is empty
            return self.memory.get(operand, "0" * 16)

    def store_in_location(self, mode, what_to_put, where_to_put):
        """
        Put given information to given place

        :param mode: 2 bit Addressing mode
        :param what_to_put: 16 bit Information to put
        :param where_to_put: 16 bit Where to put it
        :return: None
        """
        where_to_put = format(int(where_to_put, 2), "04X")
        if mode == "01":
            if where_to_put == "0001":
                self.A = what_to_put
            elif where_to_put == "0002":
                self.B = what_to_put
            elif where_to_put == "0003":
                self.C = what_to_put
            elif where_to_put == "0004":
                self.D = what_to_put
            elif where_to_put == "0005":
                self.E = what_to_put
            return
        elif mode == "10":
            where_to_put = self.get_value_in_register(where_to_put)
        self.memory[where_to_put] = what_to_put

    def addition(self, bin1, bin2):
        """
        Sums two unsigned integers and returns the result.
        Ignores most important bit and sets the carry flag if there is an overflow.

        :param bin1: a 16 bit binary number
        :param bin2: another 16 bit binary number
        :return: sum of two binary numbers, 16 bit
        """
        result = bin(int(bin1, 2) + int(bin2, 2))[2:]
        self.CF = False
        if len(result) > 16:
            self.CF = True
            result = result[1:]
        return format(int(result, 2), "016b")

    def set_zero_flag(self, num: str):
        """
        Sets the zero flag to true if number is zero, otherwise sets it to false.

        :param num: 16 bit number to check
        :return: None
        """
        is_zero = True
        for bit in num:
            if bit != "0":
                is_zero = False
                break
        if is_zero:
            self.ZF = True
        else:
            self.ZF = False

    def set_sign_flag(self, num: str):
        """
        Sets the sign flag to true if most left bit is one,
        otherwise sets it to false

        :param num: 16 bit number that will be checked
        :return: None
        """
        if num[0] == "1":
            self.SF = True
        else:
            self.SF = False

    def reverse_bits(self, num: str):
        """
        Reverse the bits in number

        :param num: Number that will get its bits reversed
        :return: Complement of given number
        """
        reversed_bits = ""
        for bit in num:
            if bit == "0":
                reversed_bits += "1"
            else:
                reversed_bits += "0"
        return reversed_bits

    # There is one function for every instruction

    def halt(self, mode, operand):
        """Halts the CPU."""
        self.halt = True

    def load(self, mode, operand):
        """Loads operand onto A."""
        operand = self.get_value(mode, operand)
        self.A = operand

    def store(self, mode, operand):
        """Stores value in A to the operand."""
        self.store_in_location(mode, self.A, operand)

    def add(self, mode, operand):
        """Adds operand to A."""
        operand = self.get_value(mode, operand)
        self.A = self.addition(operand, self.A)
        self.set_zero_flag(self.A)
        self.set_sign_flag(self.A)

    def sub(self, mode, operand):
        """Subtracts operand from A."""
        operand = self.get_value(mode, operand)
        result = self.addition(15 * "0" + "1", self.reverse_bits(operand))
        self.add("00", result)

    def inc(self, mode, operand):
        """Increments operand."""
        bits = self.get_value(mode, operand)
        plus_one = self.addition(bits, "1")
        if mode != "00":
            self.store_in_location(mode, plus_one, operand)
        self.set_zero_flag(plus_one)
        self.set_sign_flag(plus_one)

    def dec(self, mode, operand):
        """Decreases operand."""
        bits = self.get_value(mode, operand)
        # operand + NOT(1) + 1
        minus_one = self.addition(bits, "1" * 16)
        if mode != "00":
            self.store_in_location(mode, minus_one, operand)
        self.set_zero_flag(minus_one)
        self.set_sign_flag(minus_one)

    def xor(self, mode, operand):
        """Bitwise XOR operand with A and store result in A."""
        operand = self.get_value(mode, operand)
        result = ""
        for i in range(16):
            a = self.A[i] == "1"
            b = operand[i] == "1"
            if (a and not b) or (not a and b):
                result += "1"
            else:
                result += "0"
        self.A = result
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def binary_and(self, mode, operand):
        """Bitwise AND operand with A and store result in A."""
        operand = self.get_value(mode, operand)
        result = ""
        for i in range(16):
            a = self.A[i] == "1"
            b = operand[i] == "1"
            if a and b:
                result += "1"
            else:
                result += "0"
        self.A = result
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def binary_or(self, mode, operand):
        """Bitwise OR operand with A and store result in A."""
        operand = self.get_value(mode, operand)
        result = ""
        for i in range(16):
            a = self.A[i] == "1"
            b = operand[i] == "1"
            if a or b:
                result += "1"
            else:
                result += "0"
        self.A = result
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def complement(self, mode, operand):
        """Take complement of the bits of the operand."""
        bits = self.get_value(mode, operand)
        result = self.reverse_bits(bits)
        self.A = result
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def shl(self, mode, operand):
        """Shift the bits of register one position to the left."""
        bits = self.get_value(mode, operand)
        if bits[0] == "1":
            self.CF = True
        else:
            self.CF = False
        result = ""
        for i in range(1, 16):
            result += bits[i]
        result += "0"
        self.store_in_location(mode, result, operand)
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def shr(self, mode, operand):
        """Shift the bits of register one position to the right."""
        bits = self.get_value(mode, operand)
        result = "0"
        for i in range(15):
            result += bits[i]
        self.store_in_location(mode, result, operand)
        self.set_zero_flag(result)
        self.set_sign_flag(result)

    def nop(self, mode, operand):
        """No operation."""
        pass

    def push(self, mode, operand):
        """Push a word sized operand (two bytes)."""
        operand = self.get_value(mode, operand)
        self.S.append(operand)

    def pop(self, mode, operand):
        """Pop a word sized data (two bytes) into the operand."""
        element = self.S.pop()
        self.store_in_location(mode, element, operand)

    def cmp(self, mode, operand):
        """Perform comparison with A-operand and set flag accordingly"""
        initial_a = self.A
        operand = self.get_value(mode, operand)
        # Set flags like they were subtracted but keep the old values
        self.sub("00", operand)
        self.A = initial_a

    def jmp(self, mode, operand):
        """Unconditional jump. Set PC to address."""
        self.PC = int(operand, 2) // 3 - 1

    def je(self, mode, operand):
        """Jump if zero flag is true."""
        if self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def jne(self, mode, operand):
        """Jump if zero flag is false."""
        if not self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def jc(self, mode, operand):
        """Jump if carry flag is true."""
        if self.CF:
            self.PC = int(operand, 2) // 3 - 1

    def jnc(self, mode, operand):
        """Jump if carry flag is false."""
        if not self.CF:
            self.PC = int(operand, 2) // 3 - 1

    def ja(self, mode, operand):
        """Jump if above."""
        if not self.SF and not self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def jae(self, mode, operand):
        """Jump if above or equals."""
        if not self.SF or self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def jb(self, mode, operand):
        """Jump if below."""
        if self.SF and not self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def jbe(self, mode, operand):
        """Jump if below or equals."""
        if self.SF or self.ZF:
            self.PC = int(operand, 2) // 3 - 1

    def read(self, mode, operand):
        """Reads a character into the operand."""
        bits = input()
        bits = format(ord(bits[0]), "016b")
        self.store_in_location(mode, bits, operand)

    def assembly_print(self, mode, operand):
        """Prints the operand as a character"""
        operand = self.get_value(mode, operand)
        self.output.append(chr(int(operand, 2)))

    # Map opcodes to functions
    instruction_set = {
        "1": halt,
        "2": load,
        "3": store,
        "4": add,
        "5": sub,
        "6": inc,
        "7": dec,
        "8": xor,
        "9": binary_and,
        "A": binary_or,
        "B": complement,
        "C": shl,
        "D": shr,
        "E": nop,
        "F": push,
        "10": pop,
        "11": cmp,
        "12": jmp,
        "13": je,
        "14": jne,
        "15": jc,
        "16": jnc,
        "17": ja,
        "18": jae,
        "19": jb,
        "1A": jbe,
        "1B": read,
        "1C": assembly_print,
    }

    def compute(self, instructions):
        """
        Process instructions

        :return: List of printed chars
        """
        self.output = []
        while self.PC < len(instructions):
            current_instruction = instructions[self.PC]
            opcode = current_instruction[:6]
            opcode = format(int(opcode, 2), "X")
            addressing_mode = current_instruction[6:8]
            current_operand = current_instruction[8:]
            self.instruction_set[opcode](self, addressing_mode, current_operand)
            self.PC += 1
            if self.halt:
                break
        return self.output
