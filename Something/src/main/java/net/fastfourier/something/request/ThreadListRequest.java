package net.fastfourier.something.request;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.salvadordalvik.fastlibrary.util.FastUtils;
import net.fastfourier.something.data.ForumProcessTask;
import net.fastfourier.something.data.ThreadManager;
import net.fastfourier.something.list.ThreadItem;
import net.fastfourier.something.util.Constants;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by matthewshepard on 1/17/14.
 */
public class ThreadListRequest extends HTMLRequest<ThreadListRequest.ThreadListResponse> {
    private int forumId;
    private boolean scrollTo;

    public ThreadListRequest(int forumId, int page, boolean scrollTo, Response.Listener<ThreadListResponse> success, Response.ErrorListener error) {
        super(forumId == Constants.BOOKMARK_FORUMID ? "http://forums.somethingawful.com/bookmarkthreads.php" : "http://forums.somethingawful.com/forumdisplay.php", Request.Method.GET, success, error);
        if(forumId != Constants.BOOKMARK_FORUMID){
            addParam("forumid", forumId);
        }
        addParam("pagenumber", page);
        this.forumId = forumId;
        this.scrollTo = scrollTo;
    }

    @Override
    public ThreadListResponse parseHtmlResponse(NetworkResponse response, Document document) throws Exception {
        ArrayList<ThreadItem> threads = new ArrayList<ThreadItem>();
        Element threadList = document.getElementById("forum");
        for(Element thread : threadList.getElementsByClass("thread")){
            int id;
            try{
                id=Integer.parseInt(thread.id().replaceAll("\\D", ""));
            }catch (NumberFormatException nfe){
                Log.e("ThreadListRequest", "Could not parse thread ID!");
                continue;
            }

            int unread = -1, replies;

            Element seen = thread.getElementsByClass("lastseen").first();
            if(seen != null){
                Element count = seen.getElementsByClass("count").first();
                if(count != null){
                    unread = stripParseInt(count.text());
                }else{
                    unread = 0;
                }
            }

            int bookmarked = 0;
            if(thread.getElementsByClass("bm0").size() > 0){
                bookmarked = 1;
            }else if(thread.getElementsByClass("bm1").size() > 0){
                bookmarked = 2;
            }else if( thread.getElementsByClass("bm2").size() > 0){
                bookmarked = 3;
            }
            boolean closed = thread.hasClass("closed");

            replies = stripParseInt(thread.getElementsByClass("replies").first().text());

            Elements authors = thread.getElementsByClass("author");

            String author, tagUrl = null, lastPost = null;
            author = authors.first().text();
            if(authors.size() > 1){
                lastPost = authors.last().text();
            }

            Element tag = thread.getElementsByClass("icon").first();
            if(tag != null){
                tagUrl = tag.getElementsByTag("img").attr("src");
            }

            threads.add(new ThreadItem(id, getFirstTextByClass(thread, "thread_title", "Thread Title"), unread, replies, author, lastPost, bookmarked, closed, tagUrl));
        }
        if(forumId != Constants.BOOKMARK_FORUMID){
            //bookmark page doesn't have forum shortcut list
            ForumProcessTask.execute(document);
        }

        int currentPage, maxPage = 1;
        Element pages = document.getElementsByClass("pages").first();
        String pageValue = pages.getElementsByAttribute("selected").attr("value");
        if(TextUtils.isEmpty(pageValue)){
            currentPage = 1;
        }else{
            currentPage = FastUtils.safeParseInt(pageValue, 1);
        }
        Element lastPage = pages.getElementsByTag("option").last();
        if(lastPage != null){
            maxPage = FastUtils.safeParseInt(lastPage.attr("value"), 1);
        }

        ThreadManager.putThreadList(threads);

        return new ThreadListResponse(threads, currentPage, maxPage, scrollTo);
    }

    public static class ThreadListResponse{
        public final ArrayList<ThreadItem> threads;
        public final int page, maxPage;
        public final boolean scrollTo;

        public ThreadListResponse(ArrayList<ThreadItem> threads, int page, int maxPage, boolean scrollTo){
            this.threads = threads;
            this.page = page;
            this.maxPage = maxPage;
            this.scrollTo = scrollTo;
        }
    }
}