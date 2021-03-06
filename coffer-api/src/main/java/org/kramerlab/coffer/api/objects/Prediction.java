package org.kramerlab.coffer.api.objects;

import java.util.List;

import org.kramerlab.cfpminer.appdomain.ADPrediction;

public interface Prediction extends ServiceResource
{
	public String getId();

	public String getSmiles();

	public String getModelId();

	public int getPredictedIdx();

	public double[] getPredictedDistribution();

	public String getTrainingActivity();

	public List<SubgraphPredictionAttribute> getPredictionAttributes();

	public void computePredictionAttributesComputed();

	public ADPrediction getADPrediction();

}