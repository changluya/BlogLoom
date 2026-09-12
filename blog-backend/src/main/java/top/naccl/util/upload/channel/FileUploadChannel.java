package top.naccl.util.upload.channel;

import top.naccl.util.upload.UploadUtils;

/**
 * 文件上传方式
 *
 * @author: changlu
 * @date: 2026-09-13
 */
public interface FileUploadChannel {
	/**
	 * 通过指定方式上传文件
	 *
	 * @param image 需要保存的图片
	 * @return 访问图片的URL
	 * @throws Exception
	 */
	String upload(UploadUtils.ImageResource image) throws Exception;
}
