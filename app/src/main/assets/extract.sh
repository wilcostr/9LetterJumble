#!/bin/bash
exec > GRAND_DICT.txt

for f in *.txt
do
	if [ $f == "daily_challenge.txt" ]; then
		continue;
	fi
	if [ $f == "GRAND_DICT.txt" ]; then
		continue;
	fi	
	cat $f
	echo
done
