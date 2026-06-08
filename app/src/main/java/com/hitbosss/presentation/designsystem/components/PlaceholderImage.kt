package com.hitbosss.presentation.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.hitbosss.R

/**
 * Placeholder de iOS (icoEmptySquare): cuadrado gris con el isotipo "S".
 * Se usa cuando falta una foto (avatares, fotos de perfil, portadas de crear grupo/evento).
 */
@Composable
fun placeholderPainter(): Painter = painterResource(R.drawable.ic_placeholder_square)
