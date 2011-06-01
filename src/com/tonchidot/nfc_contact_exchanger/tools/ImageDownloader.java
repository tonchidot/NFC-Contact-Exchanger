/**
 * Copyright 2010 Tonchidot Corporation. All rights reserved.
 */

package com.tonchidot.nfc_contact_exchanger.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * ImageDownloader is responsible for downloading images from the internet,
 * saving them on the sd card and showing them in an ImageView. If the ImageView
 * has been reused, than the download can be canceled if not started yet. If the
 * ImageView is reused for another image, the downloaded image will not be
 * displayed. ImageDownloader uses a thread pool to download images.
 */
class ImageDownloader {

    private static final String TAG = ImageDownloader.class.getSimpleName();
    private static final int MAX_RUNNING_TASKS = 5;
    private static ConcurrentLinkedQueue<Task> queue;
    private static Dictionary<Integer, Task> tasks;
    private static int runningTasks = 0;
    private static int asyncTaskIdIncrementor = 0;
    private static Object lock = new Object();
    private static Hashtable<String, List<Task>> fileLocks;

    static {
        tasks = new Hashtable<Integer, Task>();
        queue = new ConcurrentLinkedQueue<Task>();
        fileLocks = new Hashtable<String, List<Task>>();
    }

    /**
     * Run the download in the queue. Wait if there are others running.
     * 
     * @param imageView the ImageView in which the image should be displayed
     * @param imageFileLocation the location where the image should be stored
     * @param downloadUrl the download url
     * @param manipulation the manipulation which should be done to the image
     *            before saving and displaying
     */
    public static void runInQueue(ImageView imageView, String imageFileLocation, String downloadUrl) {
        Task task = new Task();
        task.imageViewReference = new WeakReference<ImageView>(imageView);
        task.saveLocation = imageFileLocation;
        task.imageUrl = downloadUrl;

        task.id = ++asyncTaskIdIncrementor;
        imageView.setTag(task.id);
        tasks.put(task.id, task);
        queue.offer(task);

        if (runningTasks < MAX_RUNNING_TASKS) {
            new TaskExecuterThread().start();
        }
    }

    /**
     * Stop tasks for a given ImageView. This method needs to be called, when a
     * ImageView is being reused.
     * 
     * @param imageView The ImageView for which tasks should be canceled.
     */
    public static void stopTasksFor(ImageView imageView) {
        Object tag = imageView.getTag();
        if (tag instanceof Integer) {
            Task task = tasks.get(tag);
            if (task != null) {
                Log.i(TAG, "Cancelling task with id " + tag);
                task.isCanceled = true;
            }
        }
        imageView.setTag(null);
    }

    /**
     * The Class Task represents a download task
     */
    private static class Task {

        /** The handler. */
        Handler handler;

        /** The url to the image which should be downloaded. */
        String imageUrl;

        /** The save location. */
        String saveLocation;

        /** The image view reference. */
        WeakReference<ImageView> imageViewReference;

        /** The id of the task. */
        int id;

        /** Notes if the task has been canceled. */
        boolean isCanceled;

        /**
         * Instantiates a new task.
         */
        public Task() {
            handler = new Handler();
            isCanceled = false;
        }

        public String getTempFile() {
            return saveLocation + ".tmp";
        }
    }

    /**
     * The Class TaskExecuterThread is the worker thread for downloading,
     * manipulating and displaying the image.
     */
    private static class TaskExecuterThread extends Thread {

        /**
         * Instantiates a new task executer thread.
         */
        public TaskExecuterThread() {
            setPriority(Thread.MIN_PRIORITY);
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            synchronized (lock) {
                ++runningTasks;
            }

            Task task;
            while ((task = queue.poll()) != null) {
                tasks.remove(task.id);
                if (ensureFileAccessFor(task)) {
                    try {
                        if (!task.isCanceled) {
                            handleTask(task);
                        }
                    } finally {
                        // to make sure it got removed
                        fileLocks.remove(task.saveLocation);
                    }
                }
            }

            synchronized (lock) {
                if (runningTasks > 0) {
                    --runningTasks;
                }
            }
        }

        private boolean ensureFileAccessFor(Task task) {
            synchronized (fileLocks) {
                if (fileLocks.containsKey(task.saveLocation)) {
                    fileLocks.get(task.saveLocation).add(task);
                    return false;
                } else {
                    fileLocks.put(task.saveLocation, new ArrayList<ImageDownloader.Task>());
                    return true;
                }
            }
        }

        /**
         * Handle task.
         * 
         * @param task the task
         */
        private void handleTask(final Task task) {
            Log.i(TAG, "[" + task.id + "]Task started");
            try {
                downloadImage(task);
                final Bitmap bitmap = manipulateBitmap(task);
                displayBitmap(task, bitmap);
                setForDeniedDownloads(task, bitmap);
            } catch (MalformedURLException e) {
                Log.w(TAG, "MalformedURLException for " + task.saveLocation + "/" + task.imageUrl
                        + ": " + e.getMessage());
            } catch (IOException e) {
                Log.w(TAG,
                        "IOException for " + task.saveLocation + "/" + task.imageUrl + ": "
                                + e.getMessage());
            }
        }

        private void setForDeniedDownloads(Task task, Bitmap bitmap) {
            synchronized (fileLocks) {
                List<Task> tasks = fileLocks.remove(task.saveLocation);
                if (tasks != null) {
                    for (Task deniedTask : tasks) {
                        displayBitmap(deniedTask, bitmap);
                    }
                }
            }
        }

        /**
         * Manipulate bitmap.
         * 
         * @param task the task
         * @return the bitmap
         */
        private Bitmap manipulateBitmap(final Task task) {
            final Bitmap bitmap = BitmapFactory.decodeFile(task.getTempFile());
            ImageTools.saveBitmap(bitmap, task.saveLocation);
            new File(task.getTempFile()).delete();
            return bitmap;
        }

        /**
         * Download image.
         * 
         * @param task the task
         * @throws MalformedURLException the malformed url exception
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws FileNotFoundException the file not found exception
         */
        private void downloadImage(final Task task) throws MalformedURLException, IOException,
                FileNotFoundException {
            final URL theUrl = new URL(task.imageUrl);

            ImageTools.createDirsOfFile(task.saveLocation);

            BufferedInputStream in = null;
            FileOutputStream fout = null;
            try {
                in = new BufferedInputStream(theUrl.openStream());
                fout = new FileOutputStream(task.getTempFile());

                byte data[] = new byte[1024];
                int count;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    fout.write(data, 0, count);
                }
            } finally {
                if (in != null)
                    in.close();
                if (fout != null)
                    fout.close();
            }
        }

        /**
         * Display bitmap in the ImageView provided by the task.
         * 
         * @param task the task
         * @param bitmap the bitmap
         */
        private void displayBitmap(final Task task, final Bitmap bitmap) {
            task.handler.post(new Runnable() {

                @Override
                public void run() {
                    if (task.imageViewReference != null) {
                        final ImageView imageView = task.imageViewReference.get();
                        if (imageView != null && bitmap != null
                                && Integer.valueOf(task.id).equals(imageView.getTag())) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }
}
