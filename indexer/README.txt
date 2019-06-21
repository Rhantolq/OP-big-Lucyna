compilation:
	mvn clean compile assembly:single

usage:
	indexer:
		java -cp indexer-1.0-SNAPSHOT-jar-with-dependencies.jar pl.edu.mimuw.rm406247.indexer.IndexerMain
	searcher:
		java -cp indexer-1.0-SNAPSHOT-jar-with-dependencies.jar pl.edu.mimuw.rm406247.searcher.Searcher
		
Replace jar name if necessary.