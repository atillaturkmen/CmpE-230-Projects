#!/bin/bash

for ((i=0; i<=18; i++))
do
	java -jar ./mylang2ir testcases/deliverables/inputs/testcase${i}.my
	RESULT=$(llvm/lli testcases/deliverables/inputs/testcase${i}.ll)
	diff testcases/deliverables/outputs/testcase${i}.txt <(echo "$RESULT")
done