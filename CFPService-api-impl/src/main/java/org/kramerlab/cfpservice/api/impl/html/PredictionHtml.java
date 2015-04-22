package org.kramerlab.cfpservice.api.impl.html;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.kramerlab.cfpminer.CFPMiner;
import org.kramerlab.cfpservice.api.impl.Model;
import org.kramerlab.cfpservice.api.impl.Prediction;
import org.kramerlab.extendedrandomforests.weka.PredictionAttribute;
import org.mg.htmlreporting.HTMLReport;
import org.mg.javalib.datamining.ResultSet;
import org.mg.javalib.util.StringUtil;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class PredictionHtml extends ExtendedHtmlReport
{
	Prediction p;
	CFPMiner miner;

	public PredictionHtml(Prediction p)
	{
		super("Prediction of compound " + p.getSmiles(), p.getModelId(), Model.getName(p.getModelId()), p.getId(),
				"Prediction");
		setHidePageTitle(true);
		this.p = p;
		miner = Model.find(p.getModelId()).getCFPMiner();
	}

	private Renderable getPrediction(boolean hideNonMax)
	{
		return getPrediction(p, miner.getClassValues(), hideNonMax, null);
	}

	private Renderable getPrediction(double dist[], int predIdx, boolean hideNonMax)
	{
		return getPrediction(dist, miner.getClassValues(), predIdx, hideNonMax, null);
	}

	public static Renderable getPrediction(Prediction p, String classValues[], boolean hideNonMax, String url)
	{
		return getPrediction(p.getPredictedDistribution(), classValues, p.getPredictedIdx(), hideNonMax, url);
	}

	private static Renderable getPrediction(final double dist[], final String classValues[], final int predIdx,
			final boolean hideNonMax, final String url)
	{
		return new Renderable()
		{
			public void renderOn(HtmlCanvas html) throws IOException
			{
				if (url != null)
					html.a(HtmlAttributesFactory.href(url));

				boolean hide = hideNonMax && dist[predIdx] > (1 / (double) dist.length);
				for (int i = 0; i < dist.length; i++)
				{
					if (i != predIdx && hide)
						html.div(HtmlAttributesFactory.class_("smallGrey"));
					html.write(classValues[i] + " (" + StringUtil.formatDouble(dist[i] * 100) + "%)");
					if (i != predIdx && hide)
						html._div();
					else if (i < dist.length - 1)
						html.br();
				}
				if (url != null)
					html._a();
			}
		};
	}

	public static void setAdditionalInfo(HTMLReport rep, ResultSet tableSet, int rIdx, final String smiles)
			throws UnsupportedEncodingException
	{
		rep.setHeaderHelp("Info", "Looking up the compound smiles in PubChem and ChEMBL.");
		final String smi = URLEncoder.encode(smiles, "UTF-8");
		//		System.out.println(smi);
		tableSet.setResultValue(rIdx, "Info", new Renderable()
		{
			public void renderOn(HtmlCanvas html) throws IOException
			{
				html.div(HtmlAttributesFactory.class_("small"));
				html.write("Smiles: " + smiles);
				html._div();

				html.object(HtmlAttributesFactory.data("/external/all/" + smi))._object();//.width("300")
				//html.object(HtmlAttributesFactory.data("/external/pubchem/" + smi).width("300"))._object();
				//html.object(HtmlAttributesFactory.data("/external/chembl/" + smi).width("300"))._object();
			}
		});
	}

	//	public static void setTarget(HTMLReport rep, ResultSet tableSet, int rIdx, final String modelID, final String url)
	//	{
	//		tableSet.setResultValue(rIdx, "Target",
	//				rep.getExternalLink(Model.getTarget(modelID), url, ArrayUtil.toArray(Model.getDatasetURLs(modelID))));
	//	}

	public String build() throws Exception
	{
		newSection("Predicted compound");
		{
			ResultSet set = new ResultSet();
			int rIdx = set.addResult();
			//			set.setResultValue(rIdx, "Smiles", p.getSmiles());
			set.setResultValue(
					rIdx,
					"Structure",
					getImage(imageProvider.drawCompound(p.getSmiles(), molPicSize),
							imageProvider.hrefCompound(p.getSmiles()), false));
			setAdditionalInfo(this, set, rIdx, p.getSmiles());

			setTableRowsAlternating(false);
			addTable(set, false);
			setTableRowsAlternating(true);
		}
		addGap();

		newSection("Prediction model");
		{
			ResultSet set = new ResultSet();
			int rIdx = set.addResult();

			String url = "/" + p.getModelId();
			set.setResultValue(rIdx, "Dataset", HTMLReport.encodeLink(url, Model.getName(p.getModelId())));
			set.setResultValue(rIdx, "Target", HTMLReport.encodeLink(url, Model.getTarget(p.getModelId())));
			set.setResultValue(rIdx, "Prediction", getPrediction(true));
			setHeaderHelp("Prediction", text("model.prediction.tip") + " " + moreLink(DocHtml.PREDICTION_MODELS));

			setTableRowsAlternating(false);
			addTable(set);//, true);
			setTableRowsAlternating(true);
		}
		addGap();
		//		stopInlineTables();

		//addParagraph("The compound ");

		//		addGap();
		newSection("Fragments");

		startInlinesTables();
		for (final boolean match : new Boolean[] { true, false })
		{
			ResultSet set = new ResultSet();
			for (final PredictionAttribute pa : p.getPredictionAttributes())
			{
				int attIdx = pa.attribute;
				if (match == testInstanceContains(attIdx))
				{
					int rIdx = set.addResult();
					set.setResultValue(
							rIdx,
							(match ? "Present" : "Absent") + " fragments",
							getImage(getFragmentPicInTestInstance(attIdx, true, true),
									imageProvider.hrefFragment(p.getModelId(), attIdx), true));
					//					set.setResultValue(rIdx, "Value", renderer.renderAttributeValue(att, attIdx));

					Boolean moreActive = null;
					final String txt;
					final Boolean activating;

					if (pa.alternativeDistributionForInstance[miner.getActiveIdx()] != p.getPredictedDistribution()[miner
							.getActiveIdx()])
					{
						moreActive = pa.alternativeDistributionForInstance[miner.getActiveIdx()] > p
								.getPredictedDistribution()[miner.getActiveIdx()];
						if (match)
							activating = !moreActive;
						else
							activating = moreActive;

						String fragmentLink = "fragment";
						// HTMLReport.encodeLink(imageProvider.hrefFragment(p.getModelId(), attIdx),"fragment");
						String alternativePredStr = "compound would be predicted as active with "
								+ (moreActive ? "increased" : "decreased")
								+ " probability ("
								+ //
								StringUtil
										.formatDouble(pa.alternativeDistributionForInstance[miner.getActiveIdx()] * 100)
								+ "% instead of "
								+ StringUtil.formatDouble(p.getPredictedDistribution()[miner.getActiveIdx()] * 100)
								+ "%).";

						if (match)
							txt = "The " + fragmentLink + " is present in the test compound, it has "
									+ (activating ? "an activating" : "a de-activating")
									+ " effect on the prediction:<br>" + "If absent, the " + alternativePredStr + " "
									+ moreLink(DocHtml.PREDICTION_FRAGMENTS);
						else
							txt = "The " + fragmentLink + " is absent in the test compound. If present, it would have "
									+ (activating ? "an activating" : "a de-activating")
									+ " effect on the prediction:<br>" + "The " + alternativePredStr + " "
									+ moreLink(DocHtml.PREDICTION_FRAGMENTS);
					}
					else
					{
						txt = null;
						activating = null;
					}

					set.setResultValue(rIdx, "Effect", new Renderable()
					{
						public void renderOn(HtmlCanvas html) throws IOException
						{
							String effectStr = "none";
							if (activating != null)
							{
								if (activating)
									effectStr = "activating";
								else
									effectStr = "de-activating";
							}
							html.write(effectStr);
							if (activating != null)
								html.render(getMouseoverHelp(txt, " "));
							html.div(HtmlAttributesFactory.class_("smallGrey"));
							if (match)
								html.write("Prediction if absent:");
							else
								html.write("Prediction if present:");
							html.br();
							html.render(getPrediction(pa.alternativeDistributionForInstance,
									pa.alternativePredictionIdx, false));
							html._div();
						}
					});
				}
			}
			//			addParagraph((match ? "Matching" : "Not matching") + " attributes");
			addTable(set);
		}
		stopInlineTables();
		return close();
	}

	private boolean testInstanceContains(int attIdx) throws Exception
	{
		return miner.getHashcodesForTestCompound(p.getSmiles()).contains(miner.getHashcodeViaIdx(attIdx));
	}

	private String getFragmentPicInTestInstance(int attIdx, boolean fallbackToTraining, boolean crop) throws Exception
	{
		String m = p.getSmiles();
		if (!testInstanceContains(attIdx))
			//		if (testInstance.stringValue(attr).equals("0"))
			if (fallbackToTraining)
				m = miner.getTrainingDataSmiles().get(
						miner.getCompoundsForHashcode(miner.getHashcodeViaIdx(attIdx)).iterator().next());
			else
				crop = false;
		if (miner.getAtoms(m, miner.getHashcodeViaIdx(attIdx)) == null)
			throw new IllegalStateException("no atoms in " + m + " for att-idx " + attIdx + ", hashcode: "
					+ miner.getHashcodeViaIdx(attIdx));
		return imageProvider.drawCompoundWithFP(m, miner.getAtoms(m, miner.getHashcodeViaIdx(attIdx)), crop,
				crop ? croppedPicSize : molPicSize);
	}

}