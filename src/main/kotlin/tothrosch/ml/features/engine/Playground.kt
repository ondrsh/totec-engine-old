package tothrosch.ml.features.engine

/*
object Data {
	val features = getData("features")
	val labels = getData("labels")
	
	fun getData(featuresOrLabels: String): List<Variable> {
		val dir = Settings.deepLearningPath + File.separator + featuresOrLabels
		val files = Directory(dir).listFiles()
		files.sortBy { it.name.dropLast(4).toInt() }
		val variables = files[0].bufferedReader().use { it.readLine().split(",").map { Variable(it) } }
		
		files.forEach {
			it.readLines().drop(1).map {
				val split = it.split(",").map { it.toDouble() }
				for (i in split.indices) {
					variables[i].data.add(split[i])
				}
			}
		}
		
		
		return variables
	}
	
	class Variable(val label: String, val data: ArrayDeque<Number> = ArrayDeque()): Iterable<Number> by data
	
}

val features = Data.getData("features")
val labels = Data.getData("labels")*/
