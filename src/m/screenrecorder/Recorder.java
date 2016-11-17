package m.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;

public class Recorder {
	private Activity activity;
	private Point videoSize;
	private float densityDpi;
	private int bitRate;
	private String videoPath;

	private MediaProjectionManager mpm;
	private MediaRecorder mr;
	private MediaProjection mp;
	private VirtualDisplay vd;

	public Recorder(Activity activity) {
		this.activity = activity;
	}

	public void askPermission(int requestCode) {
		mpm = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		Intent captureIntent = mpm.createScreenCaptureIntent();
		activity.startActivityForResult(captureIntent, requestCode);
	}

	public boolean start(String maxFrameSize, String videoQuality, String cacheFolder,
			int resultCode, Intent data) {
		Point maxSize = getMaxSize(maxFrameSize);
		Point screeSize = getScreenSize();
		videoSize = getVideoSize(maxSize, screeSize);
		densityDpi = activity.getResources().getDisplayMetrics().densityDpi;
		densityDpi = (int) (densityDpi * screeSize.x / videoSize.x + 0.5f);
		bitRate = getBitrate(maxFrameSize, videoQuality);
		videoPath = new File(cacheFolder, System.currentTimeMillis() + ".mp4").getAbsolutePath();
		return startRecorder(resultCode, data);
	}

	private Point getMaxSize(String maxFrameSize) {
		Point size = new Point();
		String[] parts = maxFrameSize.split("_");
		size.x = Integer.parseInt(parts[1]);
		size.y = Integer.parseInt(parts[2]);
		return size;
	}

	private Point getScreenSize() {
		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getRealSize(size);
		return size;
	}

	private Point getVideoSize(Point maxSize, Point screeSize) {
		int iWidth = screeSize.x;
		int iHeight = screeSize.y;
		boolean flag = false;
		if (iWidth < iHeight) { // 交换高宽
			int tmp = iWidth;
			iWidth = iHeight;
			iHeight = tmp;
			flag = true;
		}

		int[] src = new int[] {iWidth, iHeight};
		int[] target = new int[] {maxSize.x, maxSize.y};
		if (src[0] > target[0] || src[1] > target[1]) {
			int[] dst = fixRect(src, target);
			iWidth = dst[0];
			iHeight = dst[1];
		}

		if (flag) {
			int tmp = iWidth;
			iWidth = iHeight;
			iHeight = tmp;
		}
		int dpyWidth = iWidth;
		if (iWidth % 32 > 0) {
			int delta = iWidth % 32;
			if (delta > 16) {
				dpyWidth = iWidth - delta + 32;
			} else {
				dpyWidth = iWidth - delta;
			}
		}

		int orientation = activity.getResources().getConfiguration().orientation;
		Point size = new Point();
		if (orientation == Configuration.ORIENTATION_PORTRAIT) { // 竖屏
			size.x = recalculateWidthForGPU(dpyWidth, screeSize.x);
		} else { // 横屏
			size.x = recalculateWidthForGPU(dpyWidth, screeSize.y);
		}
		size.y = (int) (size.x * screeSize.y / screeSize.x);
		size.y = size.y - size.y % 16;

		return size;
	}

	private int[] fixRect(int[] src, int[] target) {
		int[] dst = new int[2];
		float rs = (float)src[0] / (float)src[1];
		float rt = (float)target[0] / (float)target[1];
		if(rs > rt) {
			dst[0] = target[0];
			dst[1] = (int)((float)src[1] * (float)target[0] / (float)src[0] + 0.5F);
		} else {
			dst[1] = target[1];
			dst[0] = (int)((float)src[0] * (float)target[1] / (float)src[1] + 0.5F);
		}

		return dst;
	}

	private int recalculateWidthForGPU(int dpyWidth, int width) {
		int alignmentWidth = width;
		if (alignmentWidth % 32 != 0) {
			alignmentWidth = alignmentWidth - alignmentWidth % 32 + 32;
		}
		int offsetWidth = dpyWidth - alignmentWidth;
		if (offsetWidth > 0) {
			return alignmentWidth + offsetWidth - offsetWidth % 128;
		} else {
			return alignmentWidth + offsetWidth + offsetWidth % 128;
		}
	}

	private int getBitrate(String maxFrameSize, String videoQuality) {
		int br_1280_720_slow =   196608;
		int br_1280_720_vlow =   393216;
		int br_1280_720_low  =   786432;
		int br_1280_720_mid  =  1572864;
		int br_1280_720_hgh  =  3145728;
		int br_1280_720_vhgh =  6291456;
		int br_1280_720_shgh = 12582912;

		int bitRate = 0;
		if ("LEVEL_480_360".equals(maxFrameSize)) {
			if ("LEVEL_SUPER_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_slow / 3;
			} else if ("LEVEL_VERY_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_vlow / 3;
			} else if ("LEVEL_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_low / 3;
			} else if ("LEVEL_MEDIUN".equals(videoQuality)) {
				bitRate = br_1280_720_mid / 3;
			} else if ("LEVEL_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_hgh / 3;
			} else if ("LEVEL_VERY_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_vhgh / 3;
			} else {
				bitRate = br_1280_720_shgh / 3;
			}
		} else if ("LEVEL_1280_720".equals(maxFrameSize)) {
			if ("LEVEL_SUPER_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_slow;
			} else if ("LEVEL_VERY_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_vlow;
			} else if ("LEVEL_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_low;
			} else if ("LEVEL_MEDIUN".equals(videoQuality)) {
				bitRate = br_1280_720_mid;
			} else if ("LEVEL_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_hgh;
			} else if ("LEVEL_VERY_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_vhgh;
			} else {
				bitRate = br_1280_720_shgh;
			}
		} else {
			if ("LEVEL_SUPER_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_slow * 2;
			} else if ("LEVEL_VERY_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_vlow * 2;
			} else if ("LEVEL_LOW".equals(videoQuality)) {
				bitRate = br_1280_720_low * 2;
			} else if ("LEVEL_MEDIUN".equals(videoQuality)) {
				bitRate = br_1280_720_mid * 2;
			} else if ("LEVEL_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_hgh * 2;
			} else if ("LEVEL_VERY_HIGH".equals(videoQuality)) {
				bitRate = br_1280_720_vhgh * 2;
			} else {
				bitRate = br_1280_720_shgh * 2;
			}
		}

		return bitRate;
	}

	private boolean startRecorder(int resultCode, Intent data) {
		try {
			prepareRecorder();
			mp = mpm.getMediaProjection(resultCode, data);
			vd = mp.createVirtualDisplay("ScreenRecorder",
					videoSize.x, videoSize.y, (int) densityDpi,
					DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
					mr.getSurface(), null, null);
			mr.start();
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			stop();
		}
		return false;
	}

	private void prepareRecorder() throws Throwable {
		mr = new MediaRecorder();
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mr.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mr.setVideoEncodingBitRate(bitRate);
		mr.setVideoFrameRate(30);
		mr.setVideoSize(videoSize.x, videoSize.y);
		mr.setOutputFile(videoPath);
		mr.prepare();
	}

	public void stop() {
		if (mr != null) {
			try {
				mr.stop();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		if (vd != null) {
			try {
				vd.release();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			vd = null;
		}

		if (mr != null) {
			try {
				mr.release();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			mr = null;
		}

		if (mp != null) {
			try {
				mp.stop();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			mp = null;
		}
	}

}
