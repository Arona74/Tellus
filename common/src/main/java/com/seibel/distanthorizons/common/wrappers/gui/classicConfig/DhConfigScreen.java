package com.seibel.distanthorizons.common.wrappers.gui.classicConfig;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.seibel.distanthorizons.api.enums.config.DisallowSelectingViaConfigGui;
import com.seibel.distanthorizons.common.wrappers.gui.DhScreen;
import com.seibel.distanthorizons.common.wrappers.gui.GuiHelper;
import com.seibel.distanthorizons.common.wrappers.gui.TexturedButtonWidget;
import com.seibel.distanthorizons.common.wrappers.gui.config.ConfigGuiInfo;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigHandler;
import com.seibel.distanthorizons.core.config.types.*;
#if MC_VER <= MC_1_12_2
import com.seibel.distanthorizons.common.wrappers.gui.OnPressed;
#else
import com.seibel.distanthorizons.common.wrappers.gui.updater.ChangelogScreen;
#endif
import com.seibel.distanthorizons.core.config.types.enums.EConfigCommentTextPosition;
import com.seibel.distanthorizons.core.config.types.enums.EConfigValidity;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.AnnotationUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.IConfigGui;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.client.Minecraft;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
#else
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
#endif
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

#if MC_VER <= MC_1_12_2
#elif MC_VER < MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
#elif MC_VER <= MC_1_21_11
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiGraphicsExtractor;
#endif

#if MC_VER >= MC_1_17_1
import net.minecraft.client.gui.narration.NarratableEntry;
#endif

#if MC_VER <= MC_1_12_2
import net.minecraft.util.ResourceLocation;
#elif MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

import org.lwjgl.glfw.GLFW;
#if MC_VER > MC_1_12_2
import com.mojang.blaze3d.platform.InputConstants;
#endif

import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.*;
import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.Translatable;

class DhConfigScreen extends DhScreen
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final ILangWrapper LANG_WRAPPER = SingletonInjector.INSTANCE.get(ILangWrapper.class);
	
	private static final String TRANSLATION_PREFIX = ModInfo.ID + ".config.";
	
	private static final MinecraftClientWrapper MC_CLIENT = MinecraftClientWrapper.INSTANCE;
	
	
	private final #if MC_VER <= MC_1_12_2 GuiScreen #else Screen #endif parent;
	private final String category;
	private ClassicConfigGUI.ConfigListWidget configListWidget;
	private boolean reload = false;
	
	private #if MC_VER <= MC_1_12_2 GuiButton #else Button #endif doneButton;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	protected DhConfigScreen(#if MC_VER <= MC_1_12_2 GuiScreen #else Screen #endif parent, String category)
	{
		super(Translatable(
			LANG_WRAPPER.langExists(ModInfo.ID + ".config" + (category.isEmpty() ? "." + category : "") + ".title") ?
				ModInfo.ID + ".config.title" :
				ModInfo.ID + ".config" + (category.isEmpty() ? "" : "." + category) + ".title")
		);
		this.parent = parent;
		this.category = category;
	}
	
	
	#if MC_VER <= MC_1_12_2
	@Override
	public void updateScreen() { super.updateScreen(); }
	#else
	@Override
	public void tick() { super.tick(); }
	#endif

	
	
	
	//==================//
	// menu UI creation //
	//==================//
	
	#if MC_VER <= MC_1_12_2
	@Override
	public void initGui()
	#else
	@Override
	protected void init()
	#endif
	{
		#if MC_VER <= MC_1_12_2
		super.initGui();
		#else
		super.init();
		#endif
		if (!this.reload)
		{
			ConfigHandler.INSTANCE.configFileHandler.loadFromFile();
		}
		
		#if MC_VER > MC_1_12_2
		// Changelog button
		if (Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get()
			// we only have changelogs for stable builds		
			&& !ModInfo.IS_DEV_BUILD)
		{
			this.addBtn(new TexturedButtonWidget(
				// Where the button is on the screen
				this.width - 28, this.height - 28,
				// Width and height of the button
				20, 20,
				// texture UV Offset
				0, 0,
				// Some texture stuff
				0, 
				#if MC_VER < MC_1_21_1
				new ResourceLocation(ModInfo.ID, "textures/gui/changelog.png"),
				#elif MC_VER <= MC_1_21_10
				ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "textures/gui/changelog.png"),
				#else
				Identifier.fromNamespaceAndPath(ModInfo.ID, "textures/gui/changelog.png"),
				#endif
				20, 20,
				// Create the button and tell it where to go
				(buttonWidget) -> {
					ChangelogScreen changelogScreen = new ChangelogScreen(this);
					if (changelogScreen.usable)
					{
						Objects.requireNonNull(this.minecraft).setScreen(changelogScreen);
					}
					else
					{
						LOGGER.warn("Changelog was not able to open");
					}
				},
				// Add a title to the button
				Translatable(ModInfo.ID + ".updater.title")
			));
		}
		#endif
		
		
		// back button
		this.addBtn(MakeBtn(Translatable("distanthorizons.general.back"),
			(this.width / 2) - 154, this.height - 28,
			ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_HEIGHT,
			(button) ->
			{
				ConfigHandler.INSTANCE.configFileHandler.loadFromFile();
				#if MC_VER <= MC_1_12_2
				Objects.requireNonNull(this.mc).displayGuiScreen(this.parent);
				#else
				Objects.requireNonNull(this.minecraft).setScreen(this.parent);
				#endif
			}));
		
		// done/close button
		this.doneButton = this.addBtn(
			MakeBtn(Translatable("distanthorizons.general.done"),
				(this.width / 2) + 4, this.height - 28,
				ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_HEIGHT,
				(button) ->
				{
					ConfigHandler.INSTANCE.configFileHandler.saveToFile();
					#if MC_VER <= MC_1_12_2
					Objects.requireNonNull(this.mc).displayGuiScreen(this.parent);
					#else
					Objects.requireNonNull(this.minecraft).setScreen(this.parent);
					#endif
				}));
		
		this.configListWidget = new ClassicConfigGUI.ConfigListWidget(#if MC_VER <= MC_1_12_2 this.mc #else this.minecraft #endif, this.width * 2, this.height, 32, 32, 25);
		
		#if MC_VER > MC_1_12_2
		#if MC_VER < MC_1_20_6 // no background is rendered in MC 1.20.6+
		if (this.minecraft != null && this.minecraft.level != null)
		{
			this.configListWidget.setRenderBackground(false);
		}
		#endif
		
		this.addWidget(this.configListWidget);
		#endif
		
		for (AbstractConfigBase<?> configEntry : ConfigHandler.INSTANCE.configBaseList)
		{
			try
			{
				if (configEntry.getCategory().matches(this.category)
					&& configEntry.getAppearance().showInGui)
				{
					this.addMenuItem(configEntry);
				}
			}
			catch (Exception e)
			{
				String message = "ERROR: Failed to show [" + configEntry.getNameAndCategory() + "], error: [" + e.getMessage() + "]";
				if (configEntry.get() != null)
				{
					message += " with the value [" + configEntry.get() + "] with type [" + configEntry.getType() + "]";
				}
				
				LOGGER.error(message, e);
			}
		}
		
		ClassicConfigGUI.CONFIG_CORE_INTERFACE.onScreenChangeListenerList.forEach((listener) -> listener.run());
	}
	private void addMenuItem(AbstractConfigBase<?> configEntry)
	{
		trySetupConfigEntry(configEntry);
		
		if (this.tryCreateInputField(configEntry)) return;
		if (this.tryCreateCategoryButton(configEntry)) return;
		if (this.tryCreateButton(configEntry)) return;
		if (this.tryCreateComment(configEntry)) return;
		if (this.tryCreateSpacer(configEntry)) return;
		if (this.tryCreateLinkedEntry(configEntry)) return;
		
		LOGGER.warn("Config [" + configEntry.getNameAndCategory() + "] failed to show. Please try something like changing its type.");
	}
	
	private static void trySetupConfigEntry(AbstractConfigBase<?> configMenuOption)
	{
		configMenuOption.guiValue = new ConfigGuiInfo();
		Class<?> configValueClass = configMenuOption.getType();
		
		if (configMenuOption instanceof ConfigEntry)
		{
			ConfigEntry<?> configEntry = (ConfigEntry<?>) configMenuOption;
			
			if (configValueClass == Integer.class)
			{
				setupTextMenuOption(configEntry, Integer::parseInt, ClassicConfigGUI.INTEGER_ONLY_REGEX, true);
			}
			else if (configValueClass == Double.class)
			{
				setupTextMenuOption(configEntry, Double::parseDouble, ClassicConfigGUI.DECIMAL_ONLY_REGEX, false);
			}
			else if (configValueClass == Float.class)
			{
				setupTextMenuOption(configEntry, Float::parseFloat, ClassicConfigGUI.DECIMAL_ONLY_REGEX, false);
			}
			else if (configValueClass == String.class || configValueClass == List.class)
			{
				// For string or list
				setupTextMenuOption(configEntry, String::length, null, true);
			}
			else if (configValueClass == Boolean.class)
			{
				ConfigEntry<Boolean> booleanConfigEntry = (ConfigEntry<Boolean>) configEntry;
				setupBooleanMenuOption(booleanConfigEntry);
			}
			else if (configValueClass.isEnum())
			{
				ConfigEntry<Enum<?>> enumConfigEntry = (ConfigEntry<Enum<?>>) configEntry;
				Class<? extends Enum<?>> configEnumClass = (Class<? extends Enum<?>>) configValueClass;
				setupEnumMenuOption(enumConfigEntry, configEnumClass);
			}
			else
			{
				LOGGER.error("No definition for config with type: [" + configValueClass.getName() + "], for config: [" + configMenuOption.name + "].");
			}
		}
		
	}
	private static void setupTextMenuOption(AbstractConfigBase<?> configMenuOption, Function<String, Number> parsingFunc, @Nullable Pattern pattern, boolean cast)
	{
		final ConfigGuiInfo configGuiInfo = ((ConfigGuiInfo) configMenuOption.guiValue);
		
		configGuiInfo.tooltipFunction =
			(editBox, button) ->
				(stringValue) ->
				{
					boolean isNumber = (pattern != null);
					
					stringValue = stringValue.trim();
					if (!(stringValue.isEmpty() || !isNumber || pattern.matcher(stringValue).matches()))
					{
						return false;
					}
					
					
					Number numberValue = configMenuOption.typeIsFloatingPointNumber() ? 0.0 : 0; // different default values are needed so implicit casting works correctly (if not done casting from 0 (an int) to a double will cause an exception)
					configGuiInfo.errorMessage = null;
					if (isNumber
						&& !stringValue.isEmpty()
						&& !stringValue.equals("-")
						&& !stringValue.equals("."))
					{
						ConfigEntry<Number> numberConfigEntry = (ConfigEntry<Number>) configMenuOption;
						
						try
						{
							numberValue = parsingFunc.apply(stringValue);
						}
						catch (Exception e)
						{
							numberValue = null;
						}
						
						EConfigValidity validity = numberConfigEntry.getValidity(numberValue);
						switch (validity)
						{
							case VALID:
								configGuiInfo.errorMessage = null;
								break;
							case NUMBER_TOO_LOW:
								configGuiInfo.errorMessage = TextOrTranslatable("§cMinimum length is " + numberConfigEntry.getMin());
								break;
							case NUMBER_TOO_HIGH:
								configGuiInfo.errorMessage = TextOrTranslatable("§cMaximum length is " + numberConfigEntry.getMax());
								break;
							case INVALID:
								configGuiInfo.errorMessage = TextOrTranslatable("§cValue is invalid");
								break;
						}
					}
					
					editBox.setTextColor(((ConfigEntry<Number>) configMenuOption).getValidity(numberValue) == EConfigValidity.VALID ? 0xFFFFFFFF : 0xFFFF7777); // white and red
					
					
					if (configMenuOption.getType() == String.class
						|| configMenuOption.getType() == List.class)
					{
						((ConfigEntry<String>) configMenuOption).uiSetWithoutSaving(stringValue);
					}
					else if (((ConfigEntry<Number>) configMenuOption).getValidity(numberValue) == EConfigValidity.VALID)
					{
						if (!cast)
						{
							((ConfigEntry<Number>) configMenuOption).uiSetWithoutSaving(numberValue);
						}
						else
						{
							((ConfigEntry<Number>) configMenuOption).uiSetWithoutSaving(numberValue != null ? numberValue.intValue() : 0);
						}
					}
					
					return true;
				};
	}
	private static void setupBooleanMenuOption(ConfigEntry<Boolean> booleanConfigEntry)
	{
		// For boolean
		#if MC_VER <= MC_1_12_2
		Function<Object, ITextComponent> func = value -> Translatable("distanthorizons.general."+((Boolean) value ? "true" : "false")).setStyle(new Style().setColor((Boolean) value ? TextFormatting.GREEN : TextFormatting.RED));
		#else
		Function<Object, Component> func = value -> Translatable("distanthorizons.general." + ((Boolean) value ? "true" : "false")).withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
		#endif
		
		final ConfigGuiInfo configGuiInfo = ((ConfigGuiInfo) booleanConfigEntry.guiValue);
		
		#if MC_VER <= MC_1_12_2
		configGuiInfo.buttonOptionMap =
			new AbstractMap.SimpleEntry<OnPressed, Function<Object, ITextComponent>>(
				(button) ->
				{
					button.enabled = !booleanConfigEntry.apiIsOverriding();
					
					booleanConfigEntry.uiSetWithoutSaving(!booleanConfigEntry.get());
					button.displayString = func.apply(booleanConfigEntry.get()).getFormattedText();
				}, func);
		#else
		configGuiInfo.buttonOptionMap =
			new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(
				(button) ->
				{
					button.active = !booleanConfigEntry.apiIsOverriding();
					
					booleanConfigEntry.uiSetWithoutSaving(!booleanConfigEntry.get());
					button.setMessage(func.apply(booleanConfigEntry.get()));
				}, func);
		#endif
	}
	private static void setupEnumMenuOption(ConfigEntry<Enum<?>> enumConfigEntry, Class<? extends Enum<?>> enumClass)
	{
		List<Enum<?>> enumList = Arrays.asList(enumClass.getEnumConstants());
		
		final ConfigGuiInfo configGuiInfo = ((ConfigGuiInfo) enumConfigEntry.guiValue);
		#if MC_VER <= MC_1_12_2
		Function<Object, ITextComponent > getEnumTranslatableFunc = (value) -> Translatable(TRANSLATION_PREFIX + "enum." + enumClass.getSimpleName() + "." + enumConfigEntry.get().toString());
		#else
		Function<Object, Component> getEnumTranslatableFunc = (value) -> Translatable(TRANSLATION_PREFIX + "enum." + enumClass.getSimpleName() + "." + enumConfigEntry.get().toString());
		#endif
		configGuiInfo.buttonOptionMap =
			new AbstractMap.SimpleEntry<#if MC_VER <= MC_1_12_2 OnPressed #else Button.OnPress #endif, Function<Object, #if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif>>(
				(button) ->
				{
					// get the currently selected enum and enum index
					int startingIndex = enumList.indexOf(enumConfigEntry.get());
					Enum<?> enumValue = enumList.get(startingIndex);
					
					#if MC_VER <= MC_1_12_2
					boolean shiftPressed = GuiScreen.isShiftKeyDown();
					#else
					boolean shiftPressed = InputConstants.isKeyDown(MC_CLIENT.getGlfwWindowId(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(MC_CLIENT.getGlfwWindowId(), GLFW.GLFW_KEY_RIGHT_SHIFT);
					#endif
					
					// move forward or backwards depending on if the shift key is pressed
					int index = shiftPressed ? startingIndex - 1 : startingIndex + 1;
					
					// wrap around to the other side of the array when necessary
					if (index >= enumList.size())
					{
						index = 0;
					}
					else if (index < 0)
					{
						index = enumList.size() - 1;
					}
					
					
					// walk through the enums to find the next selectable one
					while (index != startingIndex)
					{
						enumValue = enumList.get(index);
						if (!AnnotationUtil.doesEnumHaveAnnotation(enumValue, DisallowSelectingViaConfigGui.class))
						{
							// this enum shouldn't be selectable via the UI,
							// skip it
							break;
						}
						
						// move forward or backwards depending on if the shift key is pressed
						index = shiftPressed ? index - 1 : index + 1;
						
						// wrap around to the other side of the array when necessary
						if (index >= enumList.size())
						{
							index = 0;
						}
						else if (index < 0)
						{
							index = enumList.size() - 1;
						}
					}
					
					
					if (index == startingIndex)
					{
						// one of the enums should be selectable, this is a programmer error
						enumValue = enumList.get(startingIndex);
						LOGGER.warn("Enum [" + enumValue.getClass() + "] doesn't contain any values that should be selectable via the UI, sticking to the currently selected value [" + enumValue + "].");
					}
					
					
					enumConfigEntry.uiSetWithoutSaving(enumValue);
					
					#if MC_VER <= MC_1_12_2
					button.enabled = !enumConfigEntry.apiIsOverriding();
					button.displayString = getEnumTranslatableFunc.apply(enumConfigEntry.get()).getFormattedText();
					#else
					button.active = !enumConfigEntry.apiIsOverriding();
					button.setMessage(getEnumTranslatableFunc.apply(enumConfigEntry.get()));
					#endif
				}, getEnumTranslatableFunc);
	}
	
	private boolean tryCreateInputField(AbstractConfigBase<?> configBase)
	{
		final ConfigGuiInfo configGuiInfo = ((ConfigGuiInfo) configBase.guiValue);
		
		if (configBase instanceof ConfigEntry)
		{
			ConfigEntry configEntry = (ConfigEntry) configBase;
			
			
			//==============//
			// reset button //
			//==============//
			
			#if MC_VER <= MC_1_12_2 OnPressed #else Button.OnPress #endif btnAction = (button) ->
			{
				configEntry.uiSetWithoutSaving(configEntry.getDefaultValue());
				this.reload = true;
				#if MC_VER <= MC_1_12_2
				Objects.requireNonNull(this.mc).displayGuiScreen(ClassicConfigGUI.getScreen(this.parent, this.category));
				#else
				Objects.requireNonNull(this.minecraft).setScreen(this.parent);
				#endif
			};
			
			int resetButtonPosX = this.width
				- ClassicConfigGUI.ConfigScreenConfigs.RESET_BUTTON_WIDTH
				- ClassicConfigGUI.ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
			int resetButtonPosZ = 0;
			
			#if MC_VER <= MC_1_12_2 GuiButton #else Button #endif resetButton = MakeBtn(
				#if MC_VER <= MC_1_12_2
				Translatable("distanthorizons.general.reset").setStyle(new Style().setColor(TextFormatting.RED)),
				#else
				Translatable("distanthorizons.general.reset").withStyle(ChatFormatting.RED),
				#endif
				resetButtonPosX, resetButtonPosZ,
				ClassicConfigGUI.ConfigScreenConfigs.RESET_BUTTON_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.RESET_BUTTON_HEIGHT,
				btnAction);
			
			if (configEntry.apiIsOverriding())
			{
				#if MC_VER <= MC_1_12_2
				resetButton.enabled = false;
				resetButton.displayString = Translatable("distanthorizons.general.apiOverride").setStyle(new Style().setColor(TextFormatting.DARK_GRAY)).getFormattedText();
				#else
				resetButton.active = false;
				resetButton.setMessage(Translatable("distanthorizons.general.apiOverride").withStyle(ChatFormatting.DARK_GRAY));
				#endif
			}
			else
			{
				resetButton.#if MC_VER <= MC_1_12_2 enabled #else active #endif = true;
			}
			
			
			
			//==============//
			// option field //
			//==============//
			
			#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif textComponent = this.GetTranslatableTextComponentForConfig(configEntry);
			
			int optionFieldPosX = this.width
				- ClassicConfigGUI.ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN
				- ClassicConfigGUI.ConfigScreenConfigs.RESET_BUTTON_WIDTH
				- ClassicConfigGUI.ConfigScreenConfigs.BUTTON_WIDTH_SPACING
				- ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_WIDTH;
			int optionFieldPosZ = 0;
			
			if (configGuiInfo.buttonOptionMap != null)
			{
				// enum/multi option input button
				#if MC_VER <= MC_1_12_2
				Map.Entry<OnPressed, Function<Object,ITextComponent>> widget = configGuiInfo.buttonOptionMap;
				#else
				Map.Entry<Button.OnPress, Function<Object, Component>> widget = configGuiInfo.buttonOptionMap;
				#endif
				if (configEntry.getType().isEnum())
				{
					widget.setValue((value) -> Translatable(TRANSLATION_PREFIX + "enum." + configEntry.getType().getSimpleName() + "." + configEntry.get().toString()));
				}
				
				#if MC_VER <= MC_1_12_2 GuiButton #else Button #endif button = MakeBtn(
					widget.getValue().apply(configEntry.get()),
					optionFieldPosX, optionFieldPosZ,
					ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
					widget.getKey());
				
				// deactivate the button if the API is overriding it
				button.#if MC_VER <= MC_1_12_2 enabled #else active #endif = !configEntry.apiIsOverriding();
				
				
				this.configListWidget.addButton(this, configEntry,
					button,
					resetButton,
					null,
					textComponent);
				
				return true;
			}
			else
			{
				// text box input
				
				#if MC_VER <= MC_1_12_2 GuiTextField #else EditBox #endif widget = new #if MC_VER <= MC_1_12_2 GuiTextField #else EditBox #endif(
					#if MC_VER <= MC_1_12_2 0, #endif
					#if MC_VER <= MC_1_12_2 this.fontRenderer #else this.font #endif,
					optionFieldPosX, optionFieldPosZ,
					ClassicConfigGUI.ConfigScreenConfigs.OPTION_FIELD_WIDTH - 4, ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT
					#if MC_VER > MC_1_12_2 ,Translatable("") #endif);
				widget.#if MC_VER <= MC_1_12_2 setMaxStringLength(3_000_000); #else setMaxLength(3_000_000); #endif // hopefully 3 million characters should be enough for any normal use-case, lol
				widget.#if MC_VER <= MC_1_12_2 setText #else insertText #endif (String.valueOf(configEntry.get()));
				
				Predicate<String> processor = configGuiInfo.tooltipFunction.apply(widget, this.doneButton);
				#if MC_VER <= MC_1_12_2
				widget.setValidator(processor::test);
				#elif MC_VER <= MC_1_21_11
				widget.setFilter(processor);
				#else
				widget.setResponder(processor::test);
				#endif
				
				this.configListWidget.addButton(this, configEntry, widget, resetButton, null, textComponent);
				
				return true;
			}
		}
		
		return false;
	}
	private boolean tryCreateCategoryButton(AbstractConfigBase<?> configType)
	{
		if (configType instanceof ConfigCategory)
		{
			ConfigCategory configCategory = (ConfigCategory) configType;
			
			#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif textComponent = this.GetTranslatableTextComponentForConfig(configCategory);
			
			int categoryPosX = this.width - ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH - ClassicConfigGUI.ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
			int categoryPosZ = this.height - ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT; // Note: the posZ value here seems to be ignored
			
			#if MC_VER <= MC_1_12_2 GuiButton #else Button #endif widget = MakeBtn(textComponent,
				categoryPosX, categoryPosZ,
				ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
				((button) ->
				{
					ConfigHandler.INSTANCE.configFileHandler.saveToFile();
					#if MC_VER <= MC_1_12_2
					Objects.requireNonNull(this.mc).displayGuiScreen(ClassicConfigGUI.getScreen(this, configCategory.getDestination()));
					#else
					Objects.requireNonNull(this.minecraft).setScreen(ClassicConfigGUI.getScreen(this, configCategory.getDestination()));
					#endif
				}));
			this.configListWidget.addButton(this, configType, widget, null, null, null);
			
			return true;
		}
		
		return false;
	}
	private boolean tryCreateButton(AbstractConfigBase<?> configType)
	{
		if (configType instanceof ConfigUIButton)
		{
			ConfigUIButton configUiButton = (ConfigUIButton) configType;
			
			#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif textComponent = this.GetTranslatableTextComponentForConfig(configUiButton);
			
			int buttonPosX = this.width - ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH - ClassicConfigGUI.ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
			
			#if MC_VER <= MC_1_12_2 GuiButton #else Button #endif widget = MakeBtn(textComponent,
				buttonPosX, this.height - 28,
				ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH, ClassicConfigGUI.ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
				(button) -> ((ConfigUIButton) configType).runAction());
			this.configListWidget.addButton(this, configType, widget, null, null, null);
			
			return true;
		}
		
		return false;
	}
	private boolean tryCreateComment(AbstractConfigBase<?> configType)
	{
		if (configType instanceof ConfigUIComment)
		{
			ConfigUIComment configUiComment = (ConfigUIComment) configType;
			
			#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif textComponent = this.GetTranslatableTextComponentForConfig(configUiComment);
			if (configUiComment.parentConfigPath != null)
			{
				textComponent = Translatable(TRANSLATION_PREFIX + configUiComment.parentConfigPath);
			}
			
			this.configListWidget.addButton(this, configType, null, null, null, textComponent);
			
			return true;
		}
		
		return false;
	}
	private boolean tryCreateSpacer(AbstractConfigBase<?> configType)
	{
		if (configType instanceof ConfigUISpacer)
		{
			#if MC_VER <= MC_1_12_2 GuiButton #else Button #endif spacerButton = MakeBtn(Translatable("distanthorizons.general.spacer"),
				10, 10, // having too small of a size causes division by 0 errors in older MC versions (IE 1.20.1)
				1, 1,
				(button) -> { });
			
			spacerButton.visible = false;
			this.configListWidget.addButton(this, configType, spacerButton, null, null, null);
			
			return true;
		}
		
		return false;
	}
	private boolean tryCreateLinkedEntry(AbstractConfigBase<?> configType)
	{
		if (configType instanceof ConfigUiLinkedEntry)
		{
			this.addMenuItem(((ConfigUiLinkedEntry) configType).get());
			
			return true;
		}
		
		return false;
	}
	
	private #if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif GetTranslatableTextComponentForConfig(AbstractConfigBase<?> configType)
	{ return Translatable(TRANSLATION_PREFIX + configType.getNameAndCategory()); }
	
	
	
	//===========//
	// rendering //
	//===========//
	
	@Override
	#if MC_VER <= MC_1_12_2
	public void drawScreen(int mouseX, int mouseY, float delta)
	#elif MC_VER < MC_1_20_1
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta)
	#elif MC_VER <= MC_1_21_11
	public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta)
	#else
	public void extractRenderState(GuiGraphicsExtractor matrices, int mouseX, int mouseY, float delta)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		this.drawDefaultBackground();
		#elif MC_VER < MC_1_20_2 // 1.20.2 now enables this by default in the `this.list.render` function
		this.renderBackground(matrices);
		#elif MC_VER <= MC_1_21_11
		super.render(matrices, mouseX, mouseY, delta);
		#else
		super.extractRenderState(matrices, mouseX, mouseY, delta);
		#endif
		
		// Render buttons
		#if MC_VER <= MC_1_12_2
		this.configListWidget.drawScreen(mouseX, mouseY, delta);
		#elif MC_VER <= MC_1_21_11
		this.configListWidget.render(matrices, mouseX, mouseY, delta);
		#else
		this.configListWidget.extractRenderState(matrices, mouseX, mouseY, delta);
		#endif
		
		
		// Render config title
		this.DhDrawCenteredString(
			#if MC_VER > MC_1_12_2	
			matrices, this.font,
			#endif
			this.title,
			this.width / 2, 15, 
			#if MC_VER < MC_1_21_6
			0xFFFFFF // RGB white
			#else 
			0xFFFFFFFF // ARGB white
			#endif);
		
		
		// render DH version
		this.DhDrawString(
			#if MC_VER > MC_1_12_2	
			matrices, this.font,
			#endif
			TextOrLiteral(ModInfo.VERSION), 2, this.height - 10, 
			#if MC_VER < MC_1_21_6
			0xAAAAAA // RGB white
			#else
			0xFFAAAAAA // ARGB white
			#endif);
		
		// If the update is pending, display this message to inform the user that it will apply when the game restarts
		if (SelfUpdater.deleteOldJarOnJvmShutdown)
		{
			this.DhDrawString(
				#if MC_VER > MC_1_12_2	
				matrices, this.font,
				#endif
				Translatable(ModInfo.ID + ".updater.waitingForClose"), 4, this.height - 42, 
				#if MC_VER < MC_1_21_6
				0xFFFFFF // RGB white
				#else
				0xFFFFFFFF // ARGB white
				#endif);
		}
		
		this.renderTooltip(
			#if MC_VER > MC_1_12_2 matrices,
			#endif
			mouseX, mouseY, delta);
		
		#if MC_VER <= MC_1_12_2
		super.drawScreen(mouseX, mouseY, delta);
		#elif MC_VER < MC_1_20_2
		super.render(matrices, mouseX, mouseY, delta);
		#endif
	}
	
	#if MC_VER <= MC_1_12_2
	private void renderTooltip(int mouseX, int mouseY, float delta)
	#elif MC_VER < MC_1_20_1
	private void renderTooltip(PoseStack matrices, int mouseX, int mouseY, float delta)
	#elif MC_VER <= MC_1_21_11
	private void renderTooltip(GuiGraphics matrices, int mouseX, int mouseY, float delta)
#else
	private void renderTooltip(GuiGraphicsExtractor matrices, int mouseX, int mouseY, float delta)
	#endif
	{
		#if MC_VER <= MC_1_12_2 Gui #else AbstractWidget #endif hoveredWidget = this.configListWidget.getHoveredButton(mouseX, mouseY);
		if (hoveredWidget == null)
		{
			return;
		}
		
		
		ClassicConfigGUI.DhButtonEntry button = ClassicConfigGUI.DhButtonEntry.BUTTON_BY_WIDGET.get(hoveredWidget);
		
		
		// A quick fix for tooltips on linked entries
		AbstractConfigBase<?> configBase = ConfigUiLinkedEntry.class.isAssignableFrom(button.dhConfigType.getClass()) ?
			((ConfigUiLinkedEntry) button.dhConfigType).get() :
			button.dhConfigType;
		
		boolean apiOverrideActive = false;
		if (configBase instanceof ConfigEntry)
		{
			apiOverrideActive = ((ConfigEntry<?>) configBase).apiIsOverriding();
		}
		
		String key = TRANSLATION_PREFIX + (configBase.category.isEmpty() ? "" : configBase.category + ".") + configBase.getName() + ".@tooltip";
		
		if (apiOverrideActive)
		{
			key = "distanthorizons.general.disabledByApi.@tooltip";
		}
		
		// display the validation error tooltip if present
		final ConfigGuiInfo configGuiInfo = ((ConfigGuiInfo) configBase.guiValue);
		if (configGuiInfo.errorMessage != null)
		{
			this.DhRenderTooltip(
				#if MC_VER > MC_1_12_2
				matrices, this.font,
				#endif
				configGuiInfo.errorMessage, mouseX, mouseY);
		}
		// display the tooltip if present
		else if (LANG_WRAPPER.langExists(key))
		{
			List<#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif> list = new ArrayList<>();
			String lang = LANG_WRAPPER.getLang(key);
			for (String langLine : lang.split("\n"))
			{
				list.add(TextOrTranslatable(langLine));
			}
			
			this.DhRenderComponentTooltip(
				#if MC_VER > MC_1_12_2
				matrices, this.font,
				#endif
				list, mouseX, mouseY);
		}
	}
	
	#if MC_VER <= MC_1_12_2
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException
	{
		super.mouseClicked(mouseX, mouseY, mouseButton);
		
		if (mouseY >= this.configListWidget.top && mouseY <= this.configListWidget.bottom)
		{
			for (ClassicConfigGUI.DhButtonEntry entry : this.configListWidget.children)
			{
				if (entry.button instanceof GuiButton btn && btn.visible)
				{
					if (btn.mousePressed(this.mc, mouseX, mouseY))
					{
						btn.playPressSound(this.mc.getSoundHandler());
						OnPressed handler = GuiHelper.HANDLER_BY_BUTTON.get(btn);
						if (handler != null) handler.pressed(btn);
					}
				}
				else if (entry.button instanceof GuiTextField field && field.getVisible())
				{
					field.mouseClicked(mouseX, mouseY, mouseButton);
				}
				
				if (entry.resetButton instanceof GuiButton reset && reset.visible)
				{
					if (reset.mousePressed(this.mc, mouseX, mouseY))
					{
						reset.playPressSound(this.mc.getSoundHandler());
						OnPressed handler = GuiHelper.HANDLER_BY_BUTTON.get(reset);
						if (handler != null) handler.pressed(reset);
					}
				}
			}
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException
	{
		super.keyTyped(typedChar, keyCode);
		for (ClassicConfigGUI.DhButtonEntry entry : this.configListWidget.children)
		{
			if (entry.button instanceof GuiTextField field)
			{
				field.textboxKeyTyped(typedChar, keyCode);
			}
		}
	}
	
	@Override
	public void handleMouseInput() throws java.io.IOException
	{
		super.handleMouseInput();
		this.configListWidget.handleMouseInput();
	}
	#endif
	
	
	//==========//
	// shutdown //
	//==========//
	
	/** When you close it, it goes to the previous screen and saves */
	@Override
	#if MC_VER <= MC_1_12_2
	public void onGuiClosed()
	#else
	public void onClose()
	#endif
	{
		ConfigHandler.INSTANCE.configFileHandler.saveToFile();
		#if MC_VER > MC_1_12_2
		Objects.requireNonNull(this.minecraft).setScreen(this.parent);
		#endif
		ClassicConfigGUI.CONFIG_CORE_INTERFACE.onScreenChangeListenerList.forEach((listener) -> listener.run());
	}
	
	
}
