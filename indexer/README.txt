compilation:
	mvn clean compile assembly:single

usage:
	indexer:
		java -cp indexer-1.0-SNAPSHOT-jar-with-dependencies.jar pl.edu.mimuw.rm406247.indexer.IndexerMain
	searcher:
		java -cp indexer-1.0-SNAPSHOT-jar-with-dependencies.jar pl.edu.mimuw.rm406247.searcher.Searcher
		
Change jar name to the name of the one generated in target/ if necessary.

Robert Michna rm406247
