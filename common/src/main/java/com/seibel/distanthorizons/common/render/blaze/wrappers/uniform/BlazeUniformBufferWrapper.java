package com.seibel.distanthorizons.common.render.blaze.wrappers.uniform;

#if MC_VER <= MC_1_21_10
public class BlazeNewUniformBufferWrapper {}

#else

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.common.render.blaze.util.BlazeUniformUtil;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class BlazeUniformBufferWrapper implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final GpuDevice GPU_DEVICE = RenderSystem.getDevice();
	private static final CommandEncoder COMMAND_ENCODER = GPU_DEVICE.createCommandEncoder();
	
	
	private final String name;
	
	/** measured in bytes */
	private int bufferSize = 0;
	
	private ByteBuffer cpuBuffer = null;
	
	private GpuBuffer gpuBuffer = null;
	public GpuBuffer getGpuBuffer() { return this.gpuBuffer; }
	private GpuBufferSlice bufferSlice = null;
	
	/** the element count the current CPU Buffer is sized for */
	private int previousElementCount = 0;
	/** how many elements are currently in flight (ie being added to the buffer right now) */
	private int elementCount = 0;
	/** used to resize the buffers dur first-time setup */
	private final ArrayList<EUniformElement> uniformElementTypes = new ArrayList<>(0);
	private Std140Builder uniformBufferBuilder = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public BlazeUniformBufferWrapper(String name) { this.name = name; }
	
	//endregion
	
	
	
	//=================//
	// element builder //
	//=================//
	//region
	
	public BlazeUniformBufferWrapper putVec2f(float x, float y)
	{
		this.putElement(EUniformElement.VEC2f);
		this.uniformBufferBuilder.putVec2(x,y);
		return this;
	}
	public BlazeUniformBufferWrapper putVec3i(int x, int y, int z)
	{
		this.putElement(EUniformElement.VEC3i);
		this.uniformBufferBuilder.putIVec3(x,y,z);
		return this;
	}
	public BlazeUniformBufferWrapper putVec3f(float x, float y, float z)
	{
		this.putElement(EUniformElement.VEC3f);
		this.uniformBufferBuilder.putVec3(x,y,z);
		return this;
	}
	public BlazeUniformBufferWrapper putVec4f(float x, float y, float z, float w)
	{
		this.putElement(EUniformElement.VEC4f);
		this.uniformBufferBuilder.putVec4(x,y,z,w);
		return this;
	}
	public BlazeUniformBufferWrapper putMat4f(Mat4f matrix)
	{
		this.putElement(EUniformElement.MAT4f);
		this.uniformBufferBuilder.putMat4f(matrix.createJomlMatrix());
		return this;
	}
	public BlazeUniformBufferWrapper putFloat(float f)
	{
		this.putElement(EUniformElement.FLOAT);
		this.uniformBufferBuilder.putFloat(f);
		return this;
	}
	public BlazeUniformBufferWrapper putInt(int i)
	{
		this.putElement(EUniformElement.INT);
		this.uniformBufferBuilder.putInt(i);
		return this;
	}
	private void putElement(EUniformElement elementTypeEnum)
	{
		this.uniformElementTypes.add(elementTypeEnum);
		
		boolean createNewBuilder = (this.elementCount == 0);
		this.elementCount++;
		if (this.elementCount > this.previousElementCount)
		{
			this.recreateCpuBuffer();
			createNewBuilder = true;
		}
		
		if (createNewBuilder)
		{
			this.uniformBufferBuilder = Std140Builder.intoBuffer(this.cpuBuffer);
		}
	}
	private void recreateCpuBuffer()
	{
		ByteBuffer oldBuffer = this.cpuBuffer;
		
		int size = calcBufferSize(this.uniformElementTypes);
		this.cpuBuffer = MemoryUtil.memAlloc(size);
		this.cpuBuffer.order(ByteOrder.nativeOrder());
		
		if (oldBuffer != null)
		{
			oldBuffer.position(0);
			this.cpuBuffer.put(oldBuffer);
			MemoryUtil.memFree(oldBuffer);
		}
		
		this.bufferSize = size;
		this.previousElementCount = this.elementCount;
	}
	private static int calcBufferSize(ArrayList<EUniformElement> uniformElements)
	{
		Std140SizeCalculator calculator = new Std140SizeCalculator();
		
		for (int i = 0; i < uniformElements.size(); i++)
		{
			EUniformElement element = uniformElements.get(i);
			switch (element)
			{
				case VEC2f -> calculator.putVec2();
				case VEC3i -> calculator.putIVec3();
				case VEC3f -> calculator.putVec3();
				case VEC4f -> calculator.putVec4();
				case MAT4f -> calculator.putMat4f();
				case INT -> calculator.putInt();
				case FLOAT -> calculator.putFloat();
				
				default -> throw new UnsupportedOperationException("No definition for element type ["+element.name()+"]");
			}
		}
		
		return calculator.get();
	}
	
	
	
	public void finishAndUpload()
	{
		// re-create the GPU buffer if needed
		GpuBuffer oldGpuBuffer = this.gpuBuffer;
		this.gpuBuffer = BlazeUniformUtil.createBuffer(this.name, this.bufferSize, this.gpuBuffer);
		
		boolean createNewBufferSlice = (this.bufferSlice == null || this.gpuBuffer != oldGpuBuffer);
		if (createNewBufferSlice)
		{
			this.bufferSlice = new GpuBufferSlice(this.gpuBuffer, 0, this.bufferSize);
		}
		
		// upload to GPU
		this.cpuBuffer.position(0);
		COMMAND_ENCODER.writeToBuffer(this.bufferSlice, this.cpuBuffer);
		
		
		// clear the element tracking for next time
		this.elementCount = 0;
		this.uniformElementTypes.clear();
		this.uniformBufferBuilder = null;
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public void close()
	{
		if (this.gpuBuffer != null)
		{
			this.gpuBuffer.close();
		}
		
		if (this.cpuBuffer != null)
		{
			MemoryUtil.memFree(this.cpuBuffer);
		}
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private enum EUniformElement
	{
		VEC2f,
		VEC3f,
		VEC3i,
		VEC4f,
		MAT4f,
		INT,
		FLOAT,
	}
	
	//endregion
	
	
}
#endif