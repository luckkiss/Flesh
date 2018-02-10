package com.ecjtu.flesh.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ecjtu.flesh.R
import com.ecjtu.flesh.cache.impl.MeiPaiCacheHelper
import com.ecjtu.flesh.cache.impl.MenuListCacheHelper
import com.ecjtu.flesh.model.ModelManager
import com.ecjtu.flesh.model.models.MeiPaiModel
import com.ecjtu.flesh.ui.adapter.MeiPaiPagerAdapter
import com.ecjtu.netcore.model.MenuModel
import com.ecjtu.netcore.network.AsyncNetwork
import com.ecjtu.netcore.network.IRequestCallback
import java.net.HttpURLConnection
import kotlin.concurrent.thread

/**
 * Created by xiang on 2018/2/8.
 */
class MeiPaiFragment : BaseTabPagerFragment() {
    companion object {
        private const val TAG = "MeiPaiFragment"
    }

    private var mLoadingDialog: AlertDialog? = null

    private var mMeiPaiMenu: List<MenuModel>? = null
    private var mMeiPaiCache: Map<String, List<MeiPaiModel>>? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.i(TAG, "onCreateView")
        return inflater?.inflate(R.layout.fragment_base_tab_pager, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
    }

    override fun onUserVisibleHintChanged(isVisibleToUser: Boolean) {
        super.onUserVisibleHintChanged(isVisibleToUser)
        if (isVisibleToUser) {
            attachTabLayout()
            val req = AsyncNetwork().apply {
                request(com.ecjtu.flesh.Constants.WEIPAI_URL, null)
                setRequestCallback(object : IRequestCallback {
                    override fun onSuccess(httpURLConnection: HttpURLConnection?, response: String) {
                        val maps = ModelManager.getMeiPaiModelByJsonString(response)
                        val menu = arrayListOf<MenuModel>()
                        for (entry in maps) {
                            val menuModel = MenuModel(entry.key, "")
                            menu.add(menuModel)
                        }

                        mLoadingDialog?.cancel()
                        mLoadingDialog = null
                        activity.runOnUiThread {
                            if (getViewPager() != null && getViewPager()?.adapter == null) {
                                getViewPager()?.adapter = MeiPaiPagerAdapter(menu, getViewPager()!!)
                                (getViewPager()?.adapter as MeiPaiPagerAdapter).setMeiPaiList(maps)
                                (getViewPager()?.adapter as MeiPaiPagerAdapter?)?.notifyDataSetChanged(true)
                            } else {
                                (getViewPager()?.adapter as MeiPaiPagerAdapter).menu = menu
                                (getViewPager()?.adapter as MeiPaiPagerAdapter).setMeiPaiList(maps)
                                (getViewPager()?.adapter as MeiPaiPagerAdapter?)?.notifyDataSetChanged(false)
                            }
                            if (userVisibleHint) {
                                attachTabLayout()
                            }
                        }
                    }
                })
            }
            if (mLoadingDialog == null) {
                Handler().post {
                    mLoadingDialog = AlertDialog.Builder(context).setTitle("加载中").setMessage("需要一小会时间")
                            .setNegativeButton("取消", { dialog, which ->
                                thread {
                                    req.cancel()
                                }
                            })
                            .setCancelable(false)
                            .setOnCancelListener {
                                mLoadingDialog = null
                            }.create()
                    mLoadingDialog?.show()
                }
            }
            thread {
                val helper = MeiPaiCacheHelper(context.filesDir.absolutePath)
                val helper2 = MenuListCacheHelper(context.filesDir.absolutePath)
                mMeiPaiMenu = helper2.get("meipaimenu")
                mMeiPaiCache = helper.get("meipaicache")
                val localMenu = mMeiPaiMenu
                val localCache = mMeiPaiCache
                activity.runOnUiThread {
                    if (localMenu != null && localCache != null) {
                        if (getViewPager() != null && getViewPager()?.adapter == null) {
                            getViewPager()?.adapter = MeiPaiPagerAdapter(localMenu, getViewPager()!!)
                            (getViewPager()?.adapter as MeiPaiPagerAdapter).setMeiPaiList(localCache as MutableMap<String, List<MeiPaiModel>>)
                            (getViewPager()?.adapter as MeiPaiPagerAdapter?)?.notifyDataSetChanged(true)
                        } else {
                            (getViewPager()?.adapter as MeiPaiPagerAdapter).menu = localMenu
                            (getViewPager()?.adapter as MeiPaiPagerAdapter).setMeiPaiList(localCache as MutableMap<String, List<MeiPaiModel>>)
                            (getViewPager()?.adapter as MeiPaiPagerAdapter?)?.notifyDataSetChanged(false)
                        }
                        if (userVisibleHint) {
                            attachTabLayout()
                        }
                        mLoadingDialog?.cancel()
                        mLoadingDialog = null

                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

    }
}