package textextract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 严格按照 基于行块分布函数的通用网页正文抽取算法 论文实现；
 * 论文重点摘抄：“由上述行块分布函数图可明显看出，正确的文本区
 * 域全都是分布函数图上含有最值且连续的一个区域，这个区域往
 * 往含有一个骤升点和一个骤降点。        
 * 于是，网页正文抽取问题转化为了求行块分布函数上的骤升
 * 骤降两个边界点，这两个边界点所含的区域包含了当前网页的行
 * 块长度最大值并且是连续的。“
 * @author liyuncong
 *
 */
public class TextExtractMe {
	// 块厚度
	private  static int blocksWidth;
	// 块长度阈值
	private static int threshold;
	
	static {
		blocksWidth = 3;
		/* 当待抽取的网页正文中遇到成块的新闻标题未剔除时，只要增大此阈值即可。*/
		/* 阈值增大，准确率提升，召回率下降；值变小，噪声会大，但可以保证抽到只有一句话的正文 */
		threshold	= 86;   
	}
	
	public static void setthreshold(int value) {
		threshold = value;
	}

	/**
	 * 
	 * @param html html源码
	 * @return 网页正文
	 */
	public static String parse(String html) {
		html = preProcess(html);
		String text = getText(html);
		return text;
	}
	
	/**
	 * 去掉所有不可能是正文的内容
	 * @param html html源码
	 * @return
	 */
	private static String preProcess(String html) {
		html = html.replaceAll("(?is)<!DOCTYPE.*?>", "");
		html = html.replaceAll("(?is)<!--.*?-->", "");				// remove html comment
		html = html.replaceAll("(?is)<script.*?>.*?</script>", ""); // remove javascript
		html = html.replaceAll("(?is)<style.*?>.*?</style>", "");   // remove css
		html = html.replaceAll("&.{2,5};|&#.{2,5};", " ");			// remove special char
		html = html.replaceAll("(?is)<.*?>", "");
		//<!--[if !IE]>|xGv00|9900d21eb16fa4350a3001b3974a9415<![endif]-->
		return html;
	}
	
	/**
	 * 
	 * @param candidate html源码被预处理之后的结果
	 * @return 网页正文
	 */
	private static String getText(String candidate) {
		List<String> lineList = Arrays.asList(candidate.split(System.lineSeparator()));
		removeAllSpace(lineList);
		
		List<Integer> indexDistribution = computeIndexDistribution(lineList);
		
		int maxLengthBlockIndex = findMaxLengthBlock(indexDistribution);
		
		// 寻找起点
		int startIndex = findStart(indexDistribution, maxLengthBlockIndex);
		// 寻找终点
		int endIndex = findEnd(indexDistribution, maxLengthBlockIndex);
	
		String text = "";
		for(int i = startIndex; i <= endIndex; i++) {
			text += lineList.get(i) + System.lineSeparator();
		}
		
		return text;
	}
	
	/**
	 * 删除掉一行文本中的所有空白
	 * @param line
	 * @return
	 */
	private static String removeSpace(String line) {
		line = line.replaceAll("\\s+", "");
		return line;
	}
	
	/**
	 * 删除多行文本中的所有空白
	 * @param lineList
	 */
	private static void removeAllSpace(List<String> lineList) {
		int size = lineList.size();
		for(int i = 0; i < size; i++) {
			String temp = removeSpace(lineList.get(i));
			lineList.set(i, temp);
		}
	}
	
	/**
	 * 
	 * @param lineList 由html预处理后的文本分解得到的所有行
	 * @return 块号-长度统计信息
	 */
	private static List<Integer> computeIndexDistribution(List<String> lineList) {
		List<Integer> indexDistribution = new ArrayList<>();
		for (int i = 0; i < lineList.size() - blocksWidth; i++) {
			int wordsNum = 0;
			// 去掉行中所有空白符，然后统计字符总数
			for (int j = i; j < i + blocksWidth; j++) { 
				wordsNum += lineList.get(j).length();
			}
			indexDistribution.add(wordsNum);
		}
		return indexDistribution;
	}
	
	/**
	 * 
	 * @param indexDistribution 块号-长度统计信息
	 * @return 长度最大的块的索引
	 */
	private static int findMaxLengthBlock(List<Integer> indexDistribution) {
		int size = indexDistribution.size();
		int maxLengthBlockIndex = 0;
		int maxLength = indexDistribution.get(0);
		for(int i = 1; i < size; i++) {
			int length = indexDistribution.get(i);
			if (length > maxLength) {
				maxLength = length;
				maxLengthBlockIndex = i;
			}
		}
		return maxLengthBlockIndex;
	}
	
	/**
	 * 从maxLengthBlockIndex向前寻找起点
	 * @param indexDistribution 块号-长度统计信息
	 * @param maxLengthBlockIndex 长度最大的块的索引
	 * @return
	 */
	private static int findStart(List<Integer> indexDistribution, int maxLengthBlockIndex) {
		int i = maxLengthBlockIndex - 1;
		for(; i >= 0; i--) {
			if (indexDistribution.get(i) < threshold) {
				break;
			}
		}
		return i + 1;
	}
	
	/**
	 * 从maxLengthBlockIndex向后寻找终点
	 * @param indexDistribution 块号-长度统计信息
	 * @param maxLengthBlockIndex 长度最大的块的索引
	 * @return
	 */
	private static int findEnd(List<Integer> indexDistribution, int maxLengthBlockIndex) {
		int size = indexDistribution.size();
		int i = maxLengthBlockIndex + 1;
		for(; i < size; i++) {
			if (indexDistribution.get(i) < threshold) {
				break;
			}
		}
		return i - 1;
	}
}
