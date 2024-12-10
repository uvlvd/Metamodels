# adapted from https://stackoverflow.com/questions/65074479/converting-all-text-files-with-multiple-encodings-in-a-directory-into-a-utf-8-en
import os, sys, codecs
import chardet
import re

base_path = "your-base-path-here"
base_path_to = "your-to-path-here"

for (dirpath, dirnames, filenames) in os.walk(base_path):
    dirpath_suffix = dirpath.removeprefix(base_path)
    current_path_to =  base_path_to + dirpath_suffix + "\\"

    for file in filenames:
        path = os.path.join(dirpath, file)
        path = str(path)
        
        path_to = os.path.join(current_path_to, file)
        path_to = str(path_to)

        f = open(path, 'rb')
        data = f.read()
        f_charInfo = chardet.detect(data)
        coding2=f_charInfo['encoding']
        coding=str(coding2)
        print(coding)
        data = f.read()

        if not re.match(r'.*\.utf-8$', coding, re.IGNORECASE): 
            print(path)
            print(coding)

            with codecs.open(path, "r", coding) as sourceFile:
                contents = sourceFile.read()

                os.makedirs(os.path.dirname(path_to), exist_ok=True) 
                with codecs.open(path_to, "w", "utf-8") as targetFile:              
                    targetFile.write(contents)
