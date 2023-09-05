import axios from "axios";
import { pickBy } from "lodash";
import { useState } from "react";
import { FieldValues, useForm } from "react-hook-form";
import { useMutation } from "react-query";
import { useNavigate } from "react-router-dom";
import { styled } from "styled-components";
import S3 from "../../aws-config";
import { COLOR } from "../../contstants/color";
import { FONT_SIZE } from "../../contstants/font";
import { API_PATHS } from "../../contstants/path";
import { FAIL, REQUIRED, SUCCESS } from "../../contstants/systemMessage";
import { useImageUpload } from "../../hooks/useImageUpload";
import { useModal } from "../../hooks/useModal";
import Button from "../common/Button";
import ImageInput from "../common/ImageInput";
import Modal from "../common/Modal";
import TextArea from "../common/TextArea";
import TextInput from "../common/TextInput";

const StyledUploadForm = styled.section`
  padding: 2.5rem 0 4rem;
  gap: 1rem;

  h1 {
    margin: 0 0 ${FONT_SIZE.font_16};
  }

  h3 {
    font-size: ${FONT_SIZE.font_24};
    margin: 0 0 0.5rem;
  }

  .basic_information {
    border: 1px solid ${COLOR.border};
    border-radius: 6px;
  }

  .description {
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid ${COLOR.border};
    background: ${COLOR.gray_100};
  }

  .box {
    padding: 1.5rem 1.5rem 2.5rem;
    display: flex;
    flex-flow: column;
    border-bottom: 1px solid ${COLOR.border};
    gap: 1rem;

    &.top > .field:first-child {
      align-items: flex-start;
    }

    .field {
      display: flex;
      flex-flow: row;
      align-items: center;
      gap: 0.75rem;

      &.radio input {
        padding: 0.5rem 0;
      }

      & > p {
        max-width: 10.125rem;
        width: 25%;
        font-size: ${FONT_SIZE.font_20};
        font-weight: 700;
      }

      .input {
        position: relative;
      }
    }

    #title {
      max-width: 37.5rem;
      width: 100%;
    }

    input[name="currentAuctionPrice"],
    input[name="immediatelyBuyPrice"],
    input[name="closingDate"] {
      box-sizing: border-box;
      max-width: 15rem;
      width: 100%;
    }
  }

  label[for="images"] {
    display: flex;
    flex-flow: column;
    align-items: center;
    justify-content: center;
    max-width: 9.375rem;
    width: 100%;
    aspect-ratio: 1/1;
    background: #f7f7f7;
  }

  input[type="file"] {
    display: none;
  }

  input[type="number"] {
    padding: 0.5rem 2rem 0.5rem 0.75rem;
    box-sizing: border-box;

    & + span {
      color: ${COLOR.gray_800};
      position: absolute;
      right: 0.75rem;
      top: 50%;
      transform: translatey(-50%);
    }
  }

  .image-input {
    display: flex;
    flex-flow: row;
    align-items: center;
    gap: 1rem;
  }

  .image-preview {
    max-width: 9.375rem;
    aspect-ratio: 1/1;
    object-fit: cover;
  }

  textarea {
    width: 100%;
    min-height: 18.75rem;
  }

  button {
    margin: 1.5rem 1.5rem 1.5rem auto;
  }
`;

const UploadForm = () => {
  const { register, handleSubmit, setError, clearErrors, formState } = useForm<FieldValues>();
  const { isOpen, setIsOpen, closeModal, toggleModal } = useModal();
  const { images, handleChange } = useImageUpload({ setError, clearErrors });
  const [isAuction, setIsAuction] = useState(true);
  const [submitResult, setSubmitResult] = useState(false);
  const navigate = useNavigate();
  const mutation = useMutation((data: FieldValues) => axios.post(API_PATHS.products(""), data, {}));

  const onSubmit = async (data: FieldValues) => {
    try {
      //s3로 보낼 이미지 준비
      const imagePaths: string[] = [];

      for (const image of images) {
        const params: AWS.S3.PutObjectRequest = {
          Bucket: "wonprice-seb45-003",
          Key: `${new Date().toISOString() + "-" + image.name}`,
          Body: image,
          ContentType: image.type,
        };

        //s3으로 이미지 업로드
        const result = await S3.upload(params).promise();
        imagePaths.push(result.Location);
      }

      //백엔드로 보낼 데이터 준비
      data = {
        ...data,
        auction: isAuction,
        images: imagePaths,
        closedAt: data.closingDate + " " + data.closingTime,
      };
      delete data.closingDate;
      delete data.closingTime;

      //값이 없는 필드 제거
      const excludeEmptyData = pickBy(data, (value, key) => key === "auction" || value.length > 0);

      //백엔드로 이미지 전송
      await mutation.mutateAsync(excludeEmptyData);
      setSubmitResult(true);
    } catch (error) {
      setSubmitResult(false);
    } finally {
      //상품 등록을 누르면 작업 결과를 알려주는 모달 출현
      setIsOpen(true);
    }
  };

  return (
    <>
      <StyledUploadForm>
        <h1>상품 등록하기</h1>

        <section className="basic_information">
          <div className="description">
            <h3>기본 정보</h3>
            <p>상품의 필수적인 정보입니다.</p>
          </div>

          <div className="box top">
            <ImageInput
              register={register}
              options={{
                required: REQUIRED.images,
              }}
              images={images}
              handleChange={handleChange}
              formState={formState}
            />

            <TextInput
              register={register}
              options={{
                required: REQUIRED.title,
              }}
              title="제목"
              id="title"
              type="text"
              formState={formState}
            />
          </div>

          <div className="description">
            <h3>판매 설정</h3>
            <p>경매 진행 여부를 선택할 수 있습니다.</p>
          </div>

          <div className="box">
            <div className="field radio">
              <p>판매 방식</p>
              <input
                type="radio"
                name="sales_method"
                id="auction"
                onClick={() => {
                  setIsAuction(true);
                }}
                defaultChecked={true}
              />
              <label htmlFor="auction">경매</label>
              <input
                type="radio"
                name="sales_method"
                id="buy_it_now"
                onClick={() => {
                  setIsAuction(false);
                }}
              />
              <label htmlFor="buy_it_now">즉시 구매</label>
            </div>

            {isAuction && (
              <TextInput
                register={register}
                options={{
                  required: REQUIRED.currentAuctionPrice,
                }}
                title="경매시작가"
                id="currentAuctionPrice"
                type="number"
                formState={formState}
              />
            )}

            <TextInput
              register={register}
              options={{
                required: REQUIRED.immediatelyBuyPrice,
              }}
              title="즉시구매가"
              id="immediatelyBuyPrice"
              type="number"
              formState={formState}
            />

            {isAuction && (
              <div className="field">
                <p>경매 종료 시간</p>
                <input
                  {...register("closingDate", {
                    required: REQUIRED.closingDate,
                  })}
                  type="date"
                />
                <input
                  {...register("closingTime", {
                    required: REQUIRED.closingTime,
                  })}
                  type="time"
                />
                {formState.errors.closingDate?.message && (
                  <p>{formState.errors.closingDate.message.toString()}</p>
                )}
                {formState.errors.closingTime?.message && (
                  <p>{formState.errors.closingTime.message.toString()}</p>
                )}
              </div>
            )}
          </div>

          <div className="description">
            <h3>상세 정보</h3>
            <p>상품 상태 등 구매에 도움이 되는 정보를 입력합니다.</p>
          </div>

          <div className="box top">
            <TextArea
              register={register}
              options={{
                required: REQUIRED.description,
              }}
              title="상품 설명"
              id="description"
              formState={formState}
            />
          </div>
          <Button
            size="big"
            design="black"
            text="상품 등록"
            type="submit"
            onClick={handleSubmit(onSubmit)}
          />
        </section>
      </StyledUploadForm>

      <Modal {...{ isOpen, setIsOpen, closeModal, toggleModal }}>
        <>
          {submitResult ? (
            <>
              <h4>상품 등록 성공</h4>
              <p>{SUCCESS.post}</p>
            </>
          ) : (
            <>
              <h4>상품 등록 실패</h4>
              <p>{FAIL.post}</p>
            </>
          )}
          <Button
            design="black"
            text="확인"
            type="button"
            onClick={() => {
              submitResult ? navigate("/product") : setIsOpen(!isOpen);
            }}
          />
        </>
      </Modal>
    </>
  );
};

export default UploadForm;