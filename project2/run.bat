@ECHO OFF

set filename=%1
set "n1=%filename%.asm"
set "n2=%filename%.bin"

python cpu230assemble.py %n1%
python cpu230exec.py %n2%