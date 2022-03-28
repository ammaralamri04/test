package com.example.myapplication

import BoundsTextView
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.core.hits.HitsView
import com.algolia.instantsearch.core.hits.connectHitsView
import com.algolia.instantsearch.core.number.range.Range
import com.algolia.instantsearch.core.searchbox.SearchBoxView
import com.algolia.instantsearch.core.searcher.Debouncer
import com.algolia.instantsearch.core.selectable.list.SelectionMode
import com.algolia.instantsearch.helper.android.filter.facet.FacetListAdapter
import com.algolia.instantsearch.helper.android.searchbox.SearchBoxViewAppCompat
import com.algolia.instantsearch.helper.filter.facet.*
import com.algolia.instantsearch.helper.filter.range.FilterRangeConnector
import com.algolia.instantsearch.helper.filter.range.FilterRangeViewModel
import com.algolia.instantsearch.helper.filter.range.connectView
import com.algolia.instantsearch.helper.filter.state.FilterGroupID
import com.algolia.instantsearch.helper.filter.state.FilterState
import com.algolia.instantsearch.helper.filter.state.groupOr
import com.algolia.instantsearch.helper.searchbox.SearchBoxConnector
import com.algolia.instantsearch.helper.searchbox.connectView
import com.algolia.instantsearch.helper.searcher.SearcherForFacets
import com.algolia.instantsearch.helper.searcher.SearcherSingleIndex
import com.algolia.instantsearch.helper.searcher.connectFilterState
import com.algolia.instantsearch.showcase.filter.facet.FacetListViewHolderImpl
import com.algolia.search.client.ClientSearch
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.Attribute
import com.algolia.search.model.IndexName
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.serialization.Serializable
import java.net.URI
import com.algolia.instantsearch.showcase.*
import com.algolia.instantsearch.helper.filter.state.filters
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.header_filter.*
import kotlinx.android.synthetic.main.showcase_filter_range.*


class SearchBox : AppCompatActivity() {
    val client = ClientSearch(
        ApplicationID(""),
        APIKey("")
    )
    val index = client.initIndex(IndexName(""))
    val searcher = SearcherSingleIndex(index)
    val searchBox = SearchBoxConnector(searcher)
    val connection = ConnectionHandler(searchBox)
    val connection2 = ConnectionHandler()
    val adapter = MovieAdapter()
    private val price = Attribute("")
    private val devicePrice = Attribute("")
    private val groupID = FilterGroupID(price)
    private val primaryBounds = 0..15
    private val secondaryBounds = 0..10
    private val initialRange = 0..15
    private val filters = filters {
        group(groupID) {
            range(price, initialRange)
        }
    }
    private val filterState = FilterState(filters)
    private val range = FilterRangeConnector(filterState, price, range = initialRange, bounds = primaryBounds)
    private val connection3 = ConnectionHandler(
        range,
        searcher.connectFilterState(filterState, Debouncer(100))
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_box)

        val searchView = findViewById<SearchView>(R.id.searchBar)
        val view: SearchBoxView = SearchBoxViewAppCompat(searchView)

        connection += searchBox.connectView(view)
        searcher.searchAsync()

        val searchResults = findViewById<RecyclerView>(R.id.searchResults)
        searchResults.adapter= adapter
        searchResults.layoutManager = LinearLayoutManager(this)
        connection2 += searcher.connectHitsView(adapter) { response ->
            response.hits.deserialize(Movie.serializer())
        }

        setContentView(R.layout.showcase_filter_range)

        connection3 += range.connectView(RangeSliderView(slider))
        connection3 += range.connectView(RangeTextView(rangeLabel))
        connection3 += range.connectView(BoundsTextView(boundsLabel))

        buttonChangeBounds.setOnClickListener {
            range.viewModel.bounds.value = Range(secondaryBounds)
            it.isEnabled = false
            buttonResetBounds.isEnabled = true
        }
        buttonResetBounds.setOnClickListener {
            range.viewModel.bounds.value = Range(primaryBounds)
            it.isEnabled = false
            buttonChangeBounds.isEnabled = true
        }

        reset.setOnClickListener {
            filterState.notify { set(filters) }
        }
        configureToolbar(toolbar)
        configureSearcher(searcher)
        onFilterChangedThenUpdateFiltersText(filterState, filtersTextView, price)
        onClearAllThenClearFilters(filterState, filtersClearAll, connection)
        onErrorThenUpdateFiltersText(searcher, filtersTextView)
        onResponseChangedThenUpdateNbHits(searcher, nbHits, connection)









        searcher.searchAsync()


    }


    override fun onDestroy() {
        super.onDestroy()
        connection.disconnect()
        searcher.cancel()

    }
}

@Serializable
data class Movie(
    val title: String
)

class MovieViewHolder(val view: TextView): RecyclerView.ViewHolder(view) {

    fun bind(data: Movie) {
        view.text = data.title
    }
}

class MovieAdapter : RecyclerView.Adapter<MovieViewHolder>(), HitsView<Movie> {

    private var movies: List<Movie> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder(TextView(parent.context))
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.bind(movie)
    }

    override fun setHits(hits: List<Movie>) {
        movies = hits
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return movies.size
    }
}