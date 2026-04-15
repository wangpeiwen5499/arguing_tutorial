const _wx = globalThis.wx
const CLOUD_ENV = 'prod-3g0nnk2k58e9785d'

/**
 * 上传文件到云托管内置 COS
 * @param {string} filePath - 本地临时文件路径
 * @param {string} cloudPath - COS 中的路径（如 "audio/1/123456.mp3"）
 * @returns {Promise<string>} fileID
 */
export function uploadToCloud(filePath, cloudPath) {
  return _wx.cloud.uploadFile({
    cloudPath,
    filePath,
    config: { env: CLOUD_ENV }
  }).then(res => res.fileID)
}
