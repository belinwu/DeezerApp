package com.albumdetail

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.designsystem.utils.UiText
import com.designsystem.utils.generatePaletteFromImage
import com.models.FavoriteSongs
import com.models.albumdetail.AlbumDetails
import com.usecases.albumdetail.AddFavoriteSongUseCase
import com.usecases.albumdetail.DeleteFavoriteSongUseCase
import com.usecases.albumdetail.GetAlbumDetailsUseCase
import com.usecases.albumdetail.GetAllFavoriteSongsUseCase
import com.usecases.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumDetailsUseCase: GetAlbumDetailsUseCase,
    private val getAllFavoriteSongsUseCase: GetAllFavoriteSongsUseCase,
    private val addFavoriteSongUseCase: AddFavoriteSongUseCase,
    private val deleteFavoriteSongUseCase: DeleteFavoriteSongUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailsUiState())
    val uiState: StateFlow<AlbumDetailsUiState> = _uiState.asStateFlow()

    init {
        getAllFavoriteSongs()
        getAlbumDetails(checkNotNull(savedStateHandle.get<Long>("album_id")))
    }

    private fun getAlbumDetails(albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(detailState = DetailsState.Loading)
            }
            getAlbumDetailsUseCase(albumId).collect { response ->
                when (response) {
                    is Response.Success -> {
                        _uiState.update {
                            it.copy(
                                albumName = response.data.title,
                                detailState = DetailsState.Success(response.data)
                            )
                        }
                    }

                    is Response.Error -> {
                        _uiState.update {
                            it.copy(
                                detailState = DetailsState.Error(
                                    message = UiText.StringResource(
                                        response.errorMessageId
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getAllFavoriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            getAllFavoriteSongsUseCase().collect { response ->
                when (response) {
                    is Response.Success -> {
                        _uiState.update {
                            it.copy(favoriteSongs = response.data)
                        }
                    }

                    is Response.Error -> {
                        _uiState.update {
                            it.copy(isDatabaseAvailable = false)
                        }
                    }
                }
            }
        }
    }

    fun addFavoriteSong(favoriteSongs: FavoriteSongs) {
        viewModelScope.launch(Dispatchers.IO) {
            addFavoriteSongUseCase(favoriteSongs).collect { response ->
                when (response) {
                    is Response.Success -> {
                        _uiState.update {
                            it.copy(userMessages = listOf())
                        }
                    }

                    is Response.Error -> {
                        _uiState.update {
                            it.copy(errorMessages = listOf(UiText.StringResource(response.errorMessageId)))
                        }
                    }
                }
            }
        }
    }

    fun removeFavoriteSong(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteFavoriteSongUseCase(songId).collect { response ->
                when (response) {
                    is Response.Success -> {
                        _uiState.update {
                            it.copy(userMessages = listOf(UiText.DynamicString("The song has been successfully added to favorites.")))
                        }
                    }

                    is Response.Error -> {
                        _uiState.update {
                            it.copy(errorMessages = listOf(UiText.StringResource(response.errorMessageId)))
                        }
                    }
                }
            }
        }
    }

    fun isSongAvailableInFavorites(songId: Long): Boolean {
        return if (_uiState.value.isDatabaseAvailable) {
            _uiState.value.favoriteSongs.any { it.id == songId }
        } else {
            false
        }
    }

    fun consumedErrorMessages() {
        _uiState.update {
            it.copy(errorMessages = listOf())
        }
    }

    fun consumedUserMessages() {
        _uiState.update {
            it.copy(userMessages = listOf())
        }
    }

    fun createPalette(bitmap: Bitmap) {
        generatePaletteFromImage(
            bitmap,
            onResult = { colorList ->
                _uiState.update {
                    it.copy(imageColor = colorList)
                }
            }
        )
    }
}

data class AlbumDetailsUiState(
    val errorMessages: List<UiText> = listOf(),
    val userMessages: List<UiText> = listOf(),
    val favoriteSongs: List<FavoriteSongs> = listOf(),
    val albumName: String = "",
    val isDatabaseAvailable: Boolean = true,
    val detailState: DetailsState = DetailsState.Loading,
    val imageColor: List<Color> = listOf(
        Color.Transparent,
        Color.Transparent
    )
)

sealed interface DetailsState {
    object Loading : DetailsState
    data class Success(val data: AlbumDetails) : DetailsState
    data class Error(val message: UiText) : DetailsState
}