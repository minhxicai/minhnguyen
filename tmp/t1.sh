echo START
for t in bash python3 unzip zip curl wget awk sed tar java; do printf "=%\
" "$t" "$t not found"; done
echo END
