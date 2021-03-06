package org.kramerlab.coffer.api.impl.persistance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.kramerlab.cfpminer.appdomain.ADInfoModel;
import org.kramerlab.coffer.api.impl.DepictService;
import org.kramerlab.coffer.api.impl.objects.AbstractModel;
import org.kramerlab.coffer.api.impl.objects.AbstractPrediction;
import org.kramerlab.coffer.api.objects.Model;
import org.mg.cdklib.cfp.CFPMiner;
import org.mg.cdklib.data.DataLoader;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;

import weka.classifiers.Classifier;

public class FilePersistanceAdapter implements PersistanceAdapter
{
	DataLoader dataLoader = DataLoader.INSTANCE;
	private static final String FOLDER = System.getProperty("user.home")
			+ "/results/coffer/persistance";

	private static String getModelFile(String id)
	{
		return FOLDER + "/model/" + id + ".model";
	}

	private static String getModelClassifierFile(String id)
	{
		return FOLDER + "/model/" + id + ".classifier";
	}

	private static String getModelCFPFile(String id)
	{
		return FOLDER + "/model/" + id + ".cfp";
	}

	private static String getModelAppDomainFile(String id)
	{
		return FOLDER + "/model/" + id + ".appdomain";
	}

	public String getModelValidationResultsFile(String id)
	{
		return FOLDER + "/model/" + id + ".res";
	}

	public String getModelValidationImageFile(String id)
	{
		return FOLDER + "/img/" + id + ".png";
	}

	public String getWarningFile(String id)
	{
		return FOLDER + "/warn/" + id + ".warn";
	}

	private static String getPredictionFile(String modelId, String predictionId)
	{
		return FOLDER + "/prediction/" + modelId + "_" + predictionId + ".prediction";
	}

	public Date getPredictionDate(String modelId, String predictionId)
	{
		return new Date(new File(getPredictionFile(modelId, predictionId)).lastModified());
	}

	public boolean modelExists(String modelId)
	{
		return new File(getModelFile(modelId)).exists();
	}

	public List<String> readDatasetEndpoints(String modelId)
	{
		return new ArrayList<String>(dataLoader.getDataset(modelId).getEndpoints());
	}

	public List<String> readDatasetSmiles(String modelId)
	{
		return new ArrayList<String>(dataLoader.getDataset(modelId).getSmiles());
	}

	public List<String> readModelDatasetWarnings(String modelId)
	{
		try
		{
			String f = getWarningFile(modelId);
			if (!new File(f).exists())
			{
				List<String> warnings = dataLoader.getDataset(modelId).getWarnings();
				System.out.println("Write warnings to " + f);
				IOUtils.writeLines(warnings, null, new FileOutputStream(f));
				return warnings;
			}
			else
				return IOUtils.readLines(new FileInputStream(f));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public AbstractModel readModel(String modelId)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(getModelFile(modelId)));
			AbstractModel m = (AbstractModel) ois.readObject();
			ois.close();
			return m;
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public Classifier readClassifier(String modelId)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(getModelClassifierFile(modelId)));
			Classifier classi = (Classifier) ois.readObject();
			ois.close();
			return classi;
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public CFPMiner readCFPMiner(String modelId)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(getModelCFPFile(modelId)));
			CFPMiner miner = (CFPMiner) ois.readObject();
			ois.close();
			return miner;
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	@Override
	public ADInfoModel readAppDomain(String modelId)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(getModelAppDomainFile(modelId)));
			ADInfoModel ad = (ADInfoModel) ois.readObject();
			ois.close();
			return ad;
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public Model[] readModels()
	{
		String models[] = new File(FOLDER + "/model").list(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".model");
			}
		});
		Model res[] = new AbstractModel[models.length];
		for (int i = 0; i < res.length; i++)
			res[i] = AbstractModel.find(FileUtil.getFilename(models[i], false));
		Arrays.sort(res, new Comparator<Model>()
		{
			public int compare(Model o1, Model o2)
			{
				return DataLoader.CFPDataComparator.compare(o1.getId(), o2.getId());
			}
		});
		return res;
	}

	public boolean predictionExists(String modelId, String predictionId)
	{
		return new File(getPredictionFile(modelId, predictionId)).exists();
	}

	public void updateDate(String modelId, String predictionId)
	{
		new File(getPredictionFile(modelId, predictionId)).setLastModified(new Date().getTime());
	}

	public AbstractPrediction readPrediction(String modelId, String predictionId)
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(getPredictionFile(modelId, predictionId)));
			AbstractPrediction pred = (AbstractPrediction) ois.readObject();
			ois.close();
			return pred;
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public String[] findAllPredictions(final String... modelIds)
	{
		final HashSet<String> checked = new HashSet<String>();
		File preds[] = new File(FOLDER + "/prediction").listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				if (!name.endsWith(".prediction"))
					return false;
				String predId = ArrayUtil.last(FileUtil.getFilename(name, false).split("_"));
				if (checked.contains(predId))
					return false;
				checked.add(predId);
				for (String modelId : modelIds)
					if (!predictionExists(modelId, predId))
						return false;
				return true;
			}
		});
		preds = ArrayUtil.sort(File.class, preds, new Comparator<File>()
		{
			public int compare(File o1, File o2)
			{
				return Long.valueOf(o2.lastModified()).compareTo(Long.valueOf(o1.lastModified()));
			}
		});
		String last[] = new String[preds.length];
		for (int i = 0; i < last.length; i++)
			last[i] = ArrayUtil.last(FileUtil.getFilename(preds[i].getName(), false).split("_"));
		return last;
	}

	public void savePrediction(AbstractPrediction prediction)
	{
		try
		{
			String file = getPredictionFile(prediction.getModelId(), prediction.getId());
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(prediction);
			oos.flush();
			oos.close();
			System.out.println("prediction written to " + file);
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public void deleteModel(String id)
	{
		System.out.println("deleting model '" + id + "' and all its predictions");
		for (String pId : findAllPredictions(id))
			deletePrediction(id, pId);
		new File(getModelCFPFile(id)).delete();
		new File(getModelClassifierFile(id)).delete();
		new File(getModelFile(id)).delete();
		new File(getModelAppDomainFile(id)).delete();
		DepictService.deleteAllImagesForModel(id);
		if (new File(getModelValidationResultsFile(id)).exists())
			new File(getModelValidationResultsFile(id)).delete();
		if (new File(getModelValidationImageFile(id)).exists())
			new File(getModelValidationImageFile(id)).delete();
	}

	public void deletePrediction(String modelId, String predictionId)
	{
		new File(getPredictionFile(modelId, predictionId)).delete();
	}

	public void saveModel(AbstractModel model)
	{
		try
		{
			String file = getModelFile(model.getId());
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(model);
			oos.flush();
			oos.close();
			System.out.println("model written to " + file);

			file = getModelClassifierFile(model.getId());
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(model.getClassifier());
			oos.flush();
			oos.close();
			System.out.println("classifier written to " + file);

			file = getModelCFPFile(model.getId());
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(model.getCFPMiner());
			oos.flush();
			oos.close();
			System.out.println("cfps written to " + file);

			file = getModelAppDomainFile(model.getId());
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(model.getAppDomain());
			oos.flush();
			oos.close();
			System.out.println("appDomain written to " + file);

			readModelDatasetWarnings(model.getId());
		}
		catch (Exception e)
		{
			throw new PersistanceException(e);
		}
	}

	public String getModelEndpoint(String modelId)
	{
		return dataLoader.getDatasetEndpoint(modelId);
	}

	public Set<String> getModelDatasetURLs(String modelId)
	{
		return dataLoader.getDatasetURLs(modelId);
	}

	public Map<String, String> getModelDatasetCitations(String modelId)
	{
		return dataLoader.getModelDatasetCitations(modelId);
	}
}
