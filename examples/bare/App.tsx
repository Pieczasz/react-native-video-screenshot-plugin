/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useState, useRef, useCallback} from 'react';
import {
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Alert,
  Image,
  Dimensions,
} from 'react-native';
import Video, {VideoRef} from 'react-native-video';
import {
  captureScreenshot,
  saveScreenshotToPath,
  isScreenshotSupported,
  getVideoDimensions,
} from 'react-native-video-screenshot-plugin';
import RNFS from 'react-native-fs';

const {width: screenWidth} = Dimensions.get('window');
const VIDEO_WIDTH = screenWidth - 32;
const VIDEO_HEIGHT = (VIDEO_WIDTH * 9) / 16; // 16:9 aspect ratio

// Sample video URLs - replace with your own
const SAMPLE_VIDEOS = [
  {
    id: 'big-buck-bunny',
    title: 'Big Buck Bunny',
    uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  },
  {
    id: 'elephant-dream',
    title: 'Elephant Dream',
    uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
  },
];

function App(): React.JSX.Element {
  const videoRef = useRef<VideoRef>(null);
  const [currentVideo, setCurrentVideo] = useState(SAMPLE_VIDEOS[0]);
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [videoSupported, setVideoSupported] = useState<boolean | null>(null);
  const [videoDimensions, setVideoDimensions] = useState<{
    width: number;
    height: number;
  } | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Handle video load
  const handleVideoLoad = useCallback(async () => {
    try {
      // Check if screenshot is supported
      const supported = await isScreenshotSupported({videoId: currentVideo.id});
      setVideoSupported(supported);

      // Get video dimensions
      const dimensions = await getVideoDimensions({videoId: currentVideo.id});
      setVideoDimensions(dimensions);
    } catch (error) {
      console.error('Failed to get video info:', error);
    }
  }, [currentVideo.id]);

  // Capture screenshot (base64 only)
  const handleCaptureScreenshot = async () => {
    try {
      setIsLoading(true);
      const result = await captureScreenshot(
        {videoId: currentVideo.id},
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          includeTimestamp: true,
        },
      );

      setScreenshot(`data:image/jpeg;base64,${result.base64}`);
      Alert.alert(
        'Screenshot Captured! 📸',
        `Size: ${result.width}x${
          result.height
        }\nTimestamp: ${result.timestamp?.toFixed(2)}s`,
        [{text: 'OK'}],
      );
    } catch (error) {
      Alert.alert('Error', `Failed to capture screenshot: ${error}`);
    } finally {
      setIsLoading(false);
    }
  };

  // Save to custom path
  const handleSaveToPath = async () => {
    try {
      setIsLoading(true);
      const documentsPath = RNFS.DocumentDirectoryPath;
      const fileName = `screenshot_${Date.now()}.jpg`;
      const filePath = `${documentsPath}/${fileName}`;

      const result = await saveScreenshotToPath(
        {videoId: currentVideo.id},
        filePath,
        {
          format: 'jpeg',
          quality: 1.0,
          includeTimestamp: true,
        },
      );

      Alert.alert(
        'Saved to Documents! 📁',
        `File: ${result.uri}\nSize: ${result.size} bytes`,
        [{text: 'OK'}],
      );
    } catch (error) {
      Alert.alert('Error', `Failed to save to path: ${error}`);
    } finally {
      setIsLoading(false);
    }
  };

  // Switch video
  const switchVideo = () => {
    const currentIndex = SAMPLE_VIDEOS.findIndex(v => v.id === currentVideo.id);
    const nextIndex = (currentIndex + 1) % SAMPLE_VIDEOS.length;
    setCurrentVideo(SAMPLE_VIDEOS[nextIndex]);
    setScreenshot(null);
    setVideoSupported(null);
    setVideoDimensions(null);
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#f8f9fa" />
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>📸 Video Screenshot Plugin</Text>
          <Text style={styles.subtitle}>
            React Native Video + Screenshot Demo
          </Text>
        </View>

        {/* Video Player */}
        <View style={styles.videoContainer}>
          <Video
            ref={videoRef}
            source={{uri: currentVideo.uri}}
            style={styles.video}
            id={currentVideo.id} // Important: ID for plugin tracking
            controls={true}
            resizeMode="contain"
            onLoad={handleVideoLoad}
            onError={error => console.error('Video error:', error)}
          />

          <View style={styles.videoInfo}>
            <Text style={styles.videoTitle}>{currentVideo.title}</Text>
            <TouchableOpacity style={styles.switchButton} onPress={switchVideo}>
              <Text style={styles.switchButtonText}>Switch Video</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Video Info */}
        {(videoSupported !== null || videoDimensions) && (
          <View style={styles.infoCard}>
            <Text style={styles.cardTitle}>Video Information</Text>
            {videoSupported !== null && (
              <Text style={styles.infoText}>
                Screenshot Support: {videoSupported ? '✅ Yes' : '❌ No'}
              </Text>
            )}
            {videoDimensions && (
              <Text style={styles.infoText}>
                Video Dimensions: {videoDimensions.width}x
                {videoDimensions.height}
              </Text>
            )}
          </View>
        )}

        {/* Action Buttons */}
        <View style={styles.actionsCard}>
          <Text style={styles.cardTitle}>Screenshot Actions</Text>

          <TouchableOpacity
            style={[styles.button, styles.primaryButton]}
            onPress={handleCaptureScreenshot}
            disabled={isLoading}>
            <Text style={styles.buttonText}>
              {isLoading ? '📸 Capturing...' : '📸 Capture Screenshot'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.tertiaryButton]}
            onPress={handleSaveToPath}
            disabled={isLoading}>
            <Text style={styles.buttonText}>
              {isLoading ? '📁 Saving...' : '📁 Save to Documents'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Screenshot Preview */}
        {screenshot && (
          <View style={styles.previewCard}>
            <Text style={styles.cardTitle}>Screenshot Preview</Text>
            <Image source={{uri: screenshot}} style={styles.previewImage} />
            <TouchableOpacity
              style={styles.clearButton}
              onPress={() => setScreenshot(null)}>
              <Text style={styles.clearButtonText}>Clear Preview</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Usage Instructions */}
        <View style={styles.instructionsCard}>
          <Text style={styles.cardTitle}>How to Use</Text>
          <Text style={styles.instructionText}>
            1. Play the video above{'\n'}
            2. Tap "Capture Screenshot" to get base64 data{'\n'}
            3. Tap "Save to Documents" to save to app documents{'\n'}
            4. Switch videos to test different sources
          </Text>
        </View>

        {/* Plugin Info */}
        <View style={styles.pluginCard}>
          <Text style={styles.cardTitle}>Plugin Features</Text>
          <Text style={styles.featureText}>✅ Base64 screenshot capture</Text>
          <Text style={styles.featureText}>✅ Save to custom paths</Text>
          <Text style={styles.featureText}>✅ JPEG and PNG formats</Text>
          <Text style={styles.featureText}>✅ Quality and size control</Text>
          <Text style={styles.featureText}>✅ Video timestamp metadata</Text>
          <Text style={styles.featureText}>
            ✅ Cross-platform (iOS & Android)
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    padding: 16,
    paddingTop: 60, // Account for status bar
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
    backgroundColor: '#ffffff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#212529',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 16,
    color: '#6c757d',
  },
  videoContainer: {
    margin: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  video: {
    width: VIDEO_WIDTH,
    height: VIDEO_HEIGHT,
  },
  videoInfo: {
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  videoTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#212529',
  },
  switchButton: {
    backgroundColor: '#007bff',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  switchButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  infoCard: {
    margin: 16,
    marginTop: 0,
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  actionsCard: {
    margin: 16,
    marginTop: 0,
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#212529',
    marginBottom: 12,
  },
  infoText: {
    fontSize: 16,
    color: '#495057',
    marginBottom: 4,
  },
  button: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    marginBottom: 12,
    alignItems: 'center',
  },
  primaryButton: {
    backgroundColor: '#007bff',
  },
  tertiaryButton: {
    backgroundColor: '#fd7e14',
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  previewCard: {
    margin: 16,
    marginTop: 0,
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  previewImage: {
    width: '100%',
    height: 200,
    borderRadius: 8,
    resizeMode: 'contain',
    backgroundColor: '#f8f9fa',
  },
  clearButton: {
    marginTop: 12,
    padding: 8,
    backgroundColor: '#6c757d',
    borderRadius: 6,
    alignItems: 'center',
  },
  clearButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  instructionsCard: {
    margin: 16,
    marginTop: 0,
    padding: 16,
    backgroundColor: '#e7f3ff',
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#007bff',
  },
  instructionText: {
    fontSize: 14,
    color: '#495057',
    lineHeight: 20,
  },
  pluginCard: {
    margin: 16,
    marginTop: 0,
    marginBottom: 32,
    padding: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#dee2e6',
  },
  featureText: {
    fontSize: 14,
    color: '#495057',
    marginBottom: 4,
  },
});

export default App;
