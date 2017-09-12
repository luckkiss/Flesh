package com.ecjtu.heaven.ui.adapter

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ecjtu.heaven.R
import com.ecjtu.heaven.cache.PageListCacheHelper
import com.ecjtu.netcore.jsoup.PageSoup
import com.ecjtu.netcore.jsoup.SoupFactory
import com.ecjtu.netcore.model.MenuModel
import com.ecjtu.netcore.model.PageModel
import com.ecjtu.netcore.network.AsyncNetwork
import com.ecjtu.netcore.network.IRequestCallback
import java.net.HttpURLConnection
import kotlin.concurrent.thread

/**
 * Created by Ethan_Xiang on 2017/9/12.
 */
class TabPagerAdapter(val menu: List<MenuModel>) : PagerAdapter() {

    private val mViewStub = HashMap<Int, VH>()

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean = view == `object`

    override fun getCount(): Int {
        return menu.size
    }

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        val item = LayoutInflater.from(container?.context).inflate(R.layout.layout_list_card_view, container, false)
        container?.addView(item)

        val helper = PageListCacheHelper(container?.context?.filesDir?.absolutePath)
        val pageModel: PageModel? = helper.get("card_cache_" + position)
        mViewStub.put(position, VH(item, menu[position], pageModel, position.toString()))
        return item
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
        container?.removeView(`object` as View)
        mViewStub.remove(position)
        onStop(container?.context!!)
    }

    override fun getPageTitle(position: Int): CharSequence {
        return menu[position].title
    }

    fun onStop(context: Context) {
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        for (entry in mViewStub) {
            val helper = PageListCacheHelper(context.filesDir.absolutePath)
            if (entry.value.getPageModel() != null) {
                helper.put("card_cache_" + entry.key, entry.value.getPageModel())
            }
            val recyclerView = entry.value.recyclerView
            if (recyclerView != null) {
                editor.putInt("last_position_${entry.key}",
                        getScrollYPosition(recyclerView)).
                        putInt("last_position_offset_${entry.key}", getScrollYOffset(recyclerView))
            }
        }
        editor.apply()
    }

    private class VH(val itemView: View, private val menu: MenuModel, pageModel: PageModel?, key: String) {
        val recyclerView = if (itemView is RecyclerView) itemView else null
        private var mPageModel: PageModel? = pageModel

        init {
            recyclerView?.layoutManager = LinearLayoutManager(recyclerView?.context, LinearLayoutManager.VERTICAL, false)
            loadCache(itemView.context, key)
            val request = AsyncNetwork()
            request.request(menu.url, null)
            request.setRequestCallback(object : IRequestCallback {
                override fun onSuccess(httpURLConnection: HttpURLConnection?, response: String) {
                    val values = SoupFactory.parseHtml(PageSoup::class.java, response)
                    if (values != null) {
                        val soups = values[PageSoup::class.java.simpleName] as PageModel
                        recyclerView?.post {
                            if (mPageModel == null) {
                                recyclerView.adapter = CardListAdapter(soups)
                                mPageModel = soups
                            } else {
                                val list = mPageModel!!.itemList
                                var needUpdate = false
                                for (item in soups.itemList) {
                                    if (list.indexOf(item) < 0) {
                                        list.add(0, item)
                                        needUpdate = true
                                    }
                                }
                                (recyclerView.adapter as CardListAdapter).pageModel = mPageModel!!
                                if (needUpdate) {
                                    recyclerView.adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            })
        }

        fun getPageModel(): PageModel? {
            return mPageModel
        }

        private fun loadCache(context: Context, key: String) {
            if (mPageModel != null) {
                recyclerView?.adapter = CardListAdapter(mPageModel!!)
                val lastPosition = PreferenceManager.getDefaultSharedPreferences(context).getInt("last_position_$key", -1)
                if (lastPosition >= 0) {
                    val yOffset = PreferenceManager.getDefaultSharedPreferences(context).getInt("last_position_offset_$key", 0)
                    (recyclerView?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastPosition, yOffset)
                }
            }
        }
    }

    fun getScrollYDistance(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val position = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleChildView = layoutManager.findViewByPosition(position)
        val itemHeight = firstVisibleChildView.height
        return position * itemHeight - (firstVisibleChildView?.top ?: 0)
    }

    fun getScrollYPosition(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findFirstVisibleItemPosition()
    }

    fun getScrollYOffset(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val position = layoutManager.findFirstVisibleItemPosition()
        val firstVisibleChildView = layoutManager.findViewByPosition(position)
        return firstVisibleChildView?.top ?: 0
    }

    override fun getItemPosition(`object`: Any?): Int {
        return POSITION_NONE
    }
}