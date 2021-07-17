package com.lagradost.cloudstream3.ui.result

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.UIHelper.hideSystemUI
import com.lagradost.cloudstream3.UIHelper.isCastApiAvailable
import com.lagradost.cloudstream3.UIHelper.isConnectedToChromecast
import com.lagradost.cloudstream3.utils.getId
import kotlinx.android.synthetic.main.result_episode.view.episode_holder
import kotlinx.android.synthetic.main.result_episode.view.episode_text
import kotlinx.android.synthetic.main.result_episode_large.view.*

const val ACTION_PLAY_EPISODE_IN_PLAYER = 1
const val ACTION_PLAY_EPISODE_IN_VLC_PLAYER = 2
const val ACTION_PLAY_EPISODE_IN_BROWSER = 3

const val ACTION_CHROME_CAST_EPISODE = 4
const val ACTION_CHROME_CAST_MIRROR = 5

const val ACTION_DOWNLOAD_EPISODE = 6
const val ACTION_DOWNLOAD_MIRROR = 7

const val ACTION_RELOAD_EPISODE = 8
const val ACTION_COPY_LINK = 9

const val ACTION_SHOW_OPTIONS = 10

data class EpisodeClickEvent(val action: Int, val data: ResultEpisode)

class EpisodeAdapter(
    var cardList: List<ResultEpisode>,
    private val clickCallback: (EpisodeClickEvent) -> Unit,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    @LayoutRes
    private var layout: Int = 0
    fun updateLayout() {
        layout = if (cardList.filter { it.poster != null }.size >= cardList.size / 2)
            R.layout.result_episode_large
        else R.layout.result_episode
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        /*val layout = if (cardList.filter { it.poster != null }.size >= cardList.size / 2)
            R.layout.result_episode_large
        else R.layout.result_episode*/

        return CardViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false),
            clickCallback
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardViewHolder -> {
                holder.bind(cardList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return cardList.size
    }

    class CardViewHolder
    constructor(
        itemView: View,
        private val clickCallback: (EpisodeClickEvent) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val episodeText: TextView = itemView.episode_text
        private val episodeRating: TextView? = itemView.episode_rating
        private val episodeDescript: TextView? = itemView.episode_descript
        private val episodeProgress: ContentLoadingProgressBar? = itemView.episode_progress
        private val episodePoster: ImageView? = itemView.episode_poster
        private val episodeDownload: ImageView? = itemView.episode_download

        private val episodeHolder = itemView.episode_holder

        @SuppressLint("SetTextI18n")
        fun bind(card: ResultEpisode) {
            val name = if (card.name == null) "Episode ${card.episode}" else "${card.episode}. ${card.name}"
            episodeText.text = name

            val watchProgress = card.getWatchProgress()

            episodeProgress?.progress = (watchProgress * 50).toInt()
            episodeProgress?.visibility = if (watchProgress > 0.0f) View.VISIBLE else View.GONE

            if (card.poster != null) {
                episodePoster?.visibility = View.VISIBLE
                if (episodePoster != null) {
                    val glideUrl =
                        GlideUrl(card.poster)
                    Glide.with(episodePoster.context)
                        .load(glideUrl)
                        .into(episodePoster)
                }
            } else {
                episodePoster?.visibility = View.GONE
            }

            if (card.rating != null) {
                episodeRating?.text = "Rated: %.1f".format(card.rating.toFloat() / 10f).replace(",", ".")
            } else {
                episodeRating?.text = ""
            }

            if (card.descript != null) {
                episodeDescript?.visibility = View.VISIBLE
                episodeDescript?.text = card.descript
            } else {
                episodeDescript?.visibility = View.GONE
            }

            episodeHolder.setOnClickListener {
                episodeHolder.context?.let { ctx ->
                    if (ctx.isConnectedToChromecast()) {
                        clickCallback.invoke(EpisodeClickEvent(ACTION_CHROME_CAST_EPISODE, card))
                    } else {
                        clickCallback.invoke(EpisodeClickEvent(ACTION_PLAY_EPISODE_IN_PLAYER, card))
                    }
                }
            }

            episodeHolder.setOnLongClickListener {
                clickCallback.invoke(EpisodeClickEvent(ACTION_SHOW_OPTIONS, card))

                return@setOnLongClickListener true
            }

            episodeDownload?.setOnClickListener {
                clickCallback.invoke(EpisodeClickEvent(ACTION_DOWNLOAD_EPISODE, card))
            }
        }
    }
}