package tothrosch.ml

import java.io.File
import java.io.FileInputStream

/*
class Predictor(fileName: String) {
	
	val evaluator = getEvaluator(fileName)
	
	
	fun evaluateSingle(input: List<Double>): Double {
		val evalMultiple = evaluateMultiple(input)
		return evalMultiple[0]
	}
	
	fun evaluateMultiple(input: List<Double>): List<Double> {
		val inputFields: List<InputField> = evaluator.inputFields
		val args = mutableMapOf<FieldName, FieldValue>()
		for (i in inputFields.indices) {
			args.put(inputFields[i].name, inputFields[i].prepare(input[i]))
		}
		val result = evaluator.evaluate(args)
		@Suppress("UNCHECKED_CAST")
		return result.entries.map { it.value } as List<Double>
	}
	
	private fun getEvaluator(fileName: String): Evaluator {
		val file = File("/home/ndrsh/$fileName");
		val inputStream = FileInputStream(file)
		val pmml = org.jpmml.model.PMMLUtil.unmarshal(inputStream)
		val battery = DefaultVisitorBattery()
		battery.applyTo(pmml)
		val evaluator: Evaluator = ModelEvaluatorBuilder(pmml).build()
		return evaluator
	}
}*/
