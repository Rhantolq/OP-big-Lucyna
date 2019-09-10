# OP-big-Lucyna
Object Oriented Programing big task.
In this project I had to implement and indexer and searcher for files and catalogues using apache lucene.

# Indexer
Indexer, in it's most complex function, monitors the given file tree (added previosly by calling "indexerMain.java --add path"
(or something like that, please check in source code or task description), checks for updated on files 
(additions, deletions, updates) and updates the index for each update (allowing searcher to search for the contents of the files).
Indexer indexes files supported by Apache Tika (or more precisely those needed for the project like pdf, txt, office files).
For other options for calling indexMain please head to code or task description 
(quick list: --add <dir>, --rm <dir>, --reindex, --list)

# Searcher
Searcher searches the index with term queries, phrase queries and fuzzy queries (and prefix queries).
It supports options like: "%lang en/pl", "%details on/off", "%limit <n>", "%color on/off", "%term", "%phrase", "%fuzzy".

# Project
The project is compiled using maven. The code is mostly documented in task description and through the variable names. 
(In code documentation was not neccessary for this project)
