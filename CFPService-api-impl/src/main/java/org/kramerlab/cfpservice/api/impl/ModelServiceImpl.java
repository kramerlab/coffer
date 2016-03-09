package org.kramerlab.cfpservice.api.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.kramerlab.cfpservice.api.FragmentObj;
import org.kramerlab.cfpservice.api.ModelService;
import org.kramerlab.cfpservice.api.PredictionObj;
import org.kramerlab.cfpservice.api.impl.html.DocHtml;
import org.kramerlab.cfpservice.api.impl.html.PredictionHtml.HideFragments;
import org.kramerlab.cfpservice.api.impl.util.CompoundInfo;
import org.mg.cdklib.CDKConverter;
import org.mg.javalib.util.StopWatchUtil;
import org.mg.javalib.util.StringUtil;
import org.springframework.stereotype.Service;

@Service("modelService#default")
public class ModelServiceImpl implements ModelService
{
	static
	{
		Locale.setDefault(Locale.US);
	}

	@Override
	public String getDocumentation()
	{
		try
		{
			return new DocHtml().build();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Model[] getModels()
	{
		return Model.listModels();
	}

	@Override
	public Model getModel(String id)
	{
		return Model.find(id);
	}

	@Override
	public Response predict(final String smiles)
	{
		try
		{
			CDKConverter.validateSmiles(smiles);
			final Model models[] = Model.listModels();
			Prediction.createPrediction(models[0], smiles, false);
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					for (int i = 1; i < models.length; i++)
						Prediction.createPrediction(models[i], smiles, false);
				}
			});
			th.start();
			return Response
					.seeOther(new URI(
							"/prediction/" + StringUtil.getMD5(smiles) + "?wait=" + models.length))
					.build();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Response predict(String id, String smiles)
	{
		try
		{
			CDKConverter.validateSmiles(smiles);
			Prediction p = Prediction.createPrediction(Model.find(id), smiles, true);
			return Response.seeOther(new URI(id + "/prediction/" + p.getId())).build();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getValidationChart(String id)
	{
		return Model.find(id).getValidationChart();
	}

	@Override
	public PredictionObj getPrediction(String modelId, String predictionId, String hideFragments,
			String maxNumFragments)
	{
		int num = maxNumFragments == null ? ModelService.DEFAULT_NUM_ENTRIES
				: Integer.parseInt(maxNumFragments);
		return Prediction.find(modelId, predictionId, HideFragments.fromString(hideFragments), num);
	}

	@Override
	public Prediction[] getPredictions(String predictionId, String wait)
	{
		int num = wait == null ? -1 : Integer.parseInt(wait);
		Prediction[] res = Prediction.find(predictionId);
		if (res.length < num) // add trailing nulls
			res = Arrays.copyOf(res, num);
		return res;
	}

	@Override
	public FragmentObj getFragment(String modelId, String fragmentId, String maxNumFragments,
			String smiles)
	{
		int num = maxNumFragments == null ? ModelService.DEFAULT_NUM_ENTRIES
				: Integer.parseInt(maxNumFragments);
		return Fragment.find(modelId, fragmentId, num, smiles);
	}

	@Override
	public String getCompoundInfo(String service, String smiles)
	{
		try
		{
			return CompoundInfo.getHTML(CompoundInfo.Service.valueOf(service), smiles);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream depict(String smiles, String size)
	{
		return DepictService.depict(smiles, size);
	}

	@Override
	public InputStream depictMatch(String smiles, String size, String atoms,
			String highlightOutgoingBonds, String activating, String crop)
	{
		return DepictService.depictMatch(smiles, size, atoms, highlightOutgoingBonds, activating,
				crop);
	}

	@Override
	public InputStream depictMultiMatch(String smiles, String size, String model)
	{
		return DepictService.depictMultiMatch(smiles, size, model);
	}

	public static void main(String[] args)
	{
		new ModelServiceImpl().predict("c1ccccc1");
		StopWatchUtil.print();
	}

}
