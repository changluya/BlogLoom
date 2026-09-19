package com.changlu.blogloom.service.storage;

/**
 * 图片上传渠道。所有上传入口按站点设置中的「上传渠道选择」获取当前实现。
 *
 * <p>渠道内的资源统一使用「相对路径」描述：本地渠道是 upload 根目录下的相对路径，
 * 阿里云渠道是 Bucket 内去除配置 path 前缀后的 Object Key。</p>
 */
public interface UploadChannel {
	String LOCAL = "local";
	String ALIYUN = "aliyun";

	/**
	 * 渠道标识，与 site_setting.uploadChannelChoose 的取值一致。
	 */
	String name();

	/**
	 * 上传资源。
	 *
	 * @param content   文件内容
	 * @param extension 文件扩展名（不含点）
	 * @param directory 渠道内业务子目录，可为空字符串表示根目录
	 * @param fileName  文件名
	 */
	UploadedResource upload(byte[] content, String extension, String directory, String fileName);

	/**
	 * 复制资源，返回目标相对路径；源资源不存在时返回 null。
	 */
	String copy(String sourceRelativePath, String targetRelativePath);

	/**
	 * 删除资源。
	 */
	void delete(String relativePath);

	/**
	 * 由相对路径构造可访问的完整 URL。
	 */
	String buildUrl(String relativePath);

	/**
	 * 将受本渠道托管的 URL 还原为相对路径；非本渠道资源返回 null。
	 */
	String toRelativePath(String url);
}
